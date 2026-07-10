package no.nordicsemi.android.toolbox.profile.parser.csc

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUInt
import kotlin.experimental.and

object CSCDataParser {
    internal var previousData: CSCDataSnapshot = CSCDataSnapshot()

    internal var wheelRevolutions: Long = -1
    internal var wheelEventTime: Int = -1
    internal var crankRevolutions: Long = -1
    internal var crankEventTime: Int = -1

    fun parse(data: ByteArray, wheelSize: WheelSize = WheelSizes.default): CSCData? {
        if (data.isEmpty()) return null

        // Decode the new data
        var offset = 0
        val flags = data[offset].also { offset += 1 }

        val wheelRevPresent = (flags and 0x01).toInt() != 0
        val crankRevPreset = (flags and 0x02).toInt() != 0

        if (data.size < 1 + (if (wheelRevPresent) 6 else 0) + (if (crankRevPreset) 4 else 0)) {
            return null
        }

        if (wheelRevPresent) {
            wheelRevolutions =
                data.getInt(offset, IntFormat.UINT32, ByteOrder.LITTLE_ENDIAN).toLong() and 0xFFFFFFFFL
            offset += 4
            wheelEventTime = data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN) // 1/1024 s
            offset += 2
        }

        if (crankRevPreset) {
            crankRevolutions = data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN).toLong() and 0xFFFFFFFFL
            offset += 2
            crankEventTime = data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN)
            offset += 2
        }

        if (!wheelRevPresent && !crankRevPreset) {
            // No data to process, return null
            return null
        }

        val wheelCircumference = wheelSize.value.toFloat()

        return CSCData(
            totalDistance = getTotalDistance(wheelCircumference),
            distance = getDistance(wheelCircumference, previousData),
            speed = getSpeed(wheelCircumference, previousData),
            wheelSize = wheelSize,
            cadence = getCrankCadence(previousData),
            gearRatio = getGearRatio(previousData),
        ).also {
            previousData = CSCDataSnapshot(
                wheelRevolutions,
                wheelEventTime,
                crankRevolutions,
                crankEventTime
            )
        }
    }

    private fun getTotalDistance(wheelCircumference: Float): Float {
        if (wheelRevolutions <= 0) {
            return 0.0f
        }
        return wheelRevolutions.toFloat() * wheelCircumference / 1000.0f // [m]
    }

    /**
     * Returns the distance traveled since the given response was received.
     *
     * @param wheelCircumference the wheel circumference in millimeters.
     * @param previous a previous response.
     * @return distance traveled since the previous response, in meters.
     */
    private fun getDistance(
        wheelCircumference: Float,
        previous: CSCDataSnapshot
    ): Float {
        if (wheelRevolutions < 0 || previous.wheelRevolutions < 0) return 0f

        val difference = wheelRevolutions - previous.wheelRevolutions
        if (difference < 0) return 0f

        return difference.toFloat() * wheelCircumference / 1000.0f
    }

    /**
     * Returns the average speed since the previous response was received.
     *
     * @param wheelCircumference the wheel circumference in millimeters.
     * @param previous a previous response.
     * @return speed in meters per second.
     */
    private fun getSpeed(
        wheelCircumference: Float,
        previous: CSCDataSnapshot
    ): Float {
        // Check for valid input
        if (wheelEventTime < 0 || previous.wheelEventTime < 0 ||
            wheelRevolutions < 0 || previous.wheelRevolutions < 0
        ) {
            return 0f
        }

        val timeDifference: Float = if (wheelEventTime < previous.wheelEventTime) {
            (65536 + wheelEventTime - previous.wheelEventTime) / 1024.0f
        } else {
            (wheelEventTime - previous.wheelEventTime) / 1024.0f
        }

        if (timeDifference == 0f) return 0f

        val distance = getDistance(wheelCircumference, previous)
        return distance / timeDifference
    }


    /**
     * Returns average wheel cadence since the previous message was received.
     *
     * @param previous a previous response.
     * @return wheel cadence in revolutions per minute.
     */
    private fun getWheelCadence(previous: CSCDataSnapshot): Float {
        if (previous.crankEventTime < 0 || crankEventTime < 0 || crankRevolutions < 0) {
            return 0f
        }
        return previous.wheelRevolutions.let { previousWheelRevolutions ->
            previous.wheelEventTime.let {
                val timeDifference: Float = if (wheelEventTime < it) {
                    (65536 + wheelEventTime - it) / 1024.0f
                } else (wheelEventTime - it) / 1024.0f // [s]

                if (timeDifference <= 0f) {
                    0.0f
                } else {
                    val revDiff = wheelRevolutions - previousWheelRevolutions
                    if (revDiff < 0) 0.0f else revDiff * 60.0f / timeDifference
                }
            }
        }
    }

    /**
     * Returns average crank cadence since the previous message was received.
     *
     * @param previous a previous response.
     * @return crank cadence in revolutions per minute.
     */
    private fun getCrankCadence(previous: CSCDataSnapshot): Float {
        val newRevs = crankRevolutions
        val newTime = crankEventTime
        val oldRevs = previous.crankRevolutions
        val oldTime = previous.crankEventTime

        if (newRevs < 0 || oldRevs < 0 || newTime < 0 || oldTime < 0) return 0f

        val revDiff = newRevs - oldRevs
        if (revDiff <= 0) return 0f

        val timeDiff: Float = if (newTime < oldTime) {
            (65536 + newTime - oldTime) / 1024.0f
        } else {
            (newTime - oldTime) / 1024.0f
        }

        if (timeDiff <= 0f) return 0f

        return revDiff * 60.0f / timeDiff
    }

    /**
     * Returns the gear ratio (equal to wheel cadence / crank cadence).
     * @param previous a previous response.
     * @return gear ratio.
     */
    private fun getGearRatio(previous: CSCDataSnapshot): Float {
        val crankCadence = getCrankCadence(previous)
        return if (crankCadence > 0) {
            getWheelCadence(previous) / crankCadence
        } else {
            0.0f
        }
    }
}
