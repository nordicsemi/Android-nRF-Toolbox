package no.nordicsemi.android.toolbox.profile.parser.hrs

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder

object HRSDataParser {

    fun parse(data: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): HRSData? {
        if (data.size < 2) return null

        var offset = 0
        val flag = data.getInt(offset, IntFormat.UINT8, byteOrder)
        val heartRateType = if (flag and 0x01 == 0) IntFormat.UINT8 else IntFormat.UINT16

        val sensorContactStatus = flag and 0x06 shr 1
        val sensorContactSupported = sensorContactStatus == 2 || sensorContactStatus == 3
        val sensorContactDetected = sensorContactStatus == 3
        val energyExpandedPresent = flag and 0x08 != 0
        val rrIntervalsPresent = flag and 0x10 != 0
        offset += 1

        // Validate packet length
        if (data.size < (1 + (heartRateType.length) + (if (energyExpandedPresent) 2 else 0) + if (rrIntervalsPresent) 2 else 0)) {
            return null
        }
        // Prepare data
        val sensorContact = if (sensorContactSupported) sensorContactDetected else false

        val heartRate: Int = data.getInt(offset, heartRateType, byteOrder)
        offset += heartRateType.length

        var energyExpanded: Int? = null
        if (energyExpandedPresent) {
            energyExpanded = data.getInt(offset, IntFormat.UINT16, byteOrder)
            offset += 2
        }

        val rrIntervals = if (rrIntervalsPresent) {
            val count: Int = (data.size - offset) / 2
            val intervals: MutableList<Int> = ArrayList(count)
            for (i in 0 until count) {
                intervals.add(data.getInt(offset, IntFormat.UINT16, byteOrder))
                offset += 2
            }
            intervals.toList()
        } else {
            emptyList()
        }

        return HRSData(heartRate, sensorContact, energyExpanded, rrIntervals)
    }
}
