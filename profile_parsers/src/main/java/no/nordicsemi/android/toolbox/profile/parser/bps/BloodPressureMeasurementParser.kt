package no.nordicsemi.android.toolbox.profile.parser.bps

import no.nordicsemi.android.toolbox.profile.parser.date.DateTimeParser
import no.nordicsemi.kotlin.data.FloatFormat
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getFloat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder
import java.util.Calendar

object BloodPressureMeasurementParser {

    fun parse(data: ByteArray): BloodPressureMeasurementData? {
        if (data.size < 7) return null

        var offset = 0
        val flags: Int = data.getInt(offset, IntFormat.UINT8, ByteOrder.LITTLE_ENDIAN)
            .also { offset++ }

        // See UNIT_* for unit options
        val unit: BloodPressureType = if (flags and 0x01 == BloodPressureType.UNIT_KPA.value) {
            BloodPressureType.UNIT_KPA
        } else {
            BloodPressureType.UNIT_MMHG
        }
        val timestampPresent = flags and 0x02 != 0
        val pulseRatePresent = flags and 0x04 != 0
        val userIdPresent = flags and 0x08 != 0
        val measurementStatusPresent = flags and 0x10 != 0

        if (data.size < (7
                    + (if (timestampPresent) 7 else 0) + (if (pulseRatePresent) 2 else 0)
                    + (if (userIdPresent) 1 else 0) + if (measurementStatusPresent) 2 else 0)
        ) {
            return null
        }

        // Following bytes - systolic, diastolic and mean arterial pressure
        val systolic = data.getFloat(offset, FloatFormat.IEEE_11073_16_BIT, ByteOrder.LITTLE_ENDIAN)
        val diastolic = data.getFloat(offset + 2, FloatFormat.IEEE_11073_16_BIT, ByteOrder.LITTLE_ENDIAN)
        val meanArterialPressure = data.getFloat(offset + 4, FloatFormat.IEEE_11073_16_BIT, ByteOrder.LITTLE_ENDIAN)
        offset += 6

        // Parse timestamp if present
        var calendar: Calendar? = null
        if (timestampPresent) {
            calendar = DateTimeParser.parse(data, offset)
            offset += 7
        }

        // Parse pulse rate if present
        var pulseRate: Float? = null
        if (pulseRatePresent) {
            pulseRate = data.getFloat(offset, FloatFormat.IEEE_11073_16_BIT, ByteOrder.LITTLE_ENDIAN)
            offset += 2
        }

        // Read user id if present
        var userId: Int? = null
        if (userIdPresent) {
            userId = data.getInt(offset, IntFormat.UINT8)
            offset += 1
        }

        // Read measurement status if present
        var status: BPMStatus? = null
        if (measurementStatusPresent) {
            val measurementStatus: Int = data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN)
            // offset += 2;
            status = BPMStatus(measurementStatus)
        }

        return BloodPressureMeasurementData(
            systolic,
            diastolic,
            meanArterialPressure,
            unit,
            pulseRate,
            userId,
            status,
            calendar
        )
    }
}