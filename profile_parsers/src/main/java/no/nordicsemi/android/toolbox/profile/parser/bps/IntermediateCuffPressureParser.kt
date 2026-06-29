package no.nordicsemi.android.toolbox.profile.parser.bps

import no.nordicsemi.android.toolbox.profile.parser.date.DateTimeParser
import no.nordicsemi.kotlin.data.FloatFormat
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getFloat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder
import java.util.Calendar

object IntermediateCuffPressureParser {

    fun parse(data: ByteArray): IntermediateCuffPressureData? {
        if (data.size < 7) return null

        // First byte: flags
        var offset = 0
        val flags: Int = data.getInt(offset++, IntFormat.UINT8)

        // See UNIT_* for unit options
        val unit = if (flags and 0x01 == BloodPressureType.UNIT_KPA.value) {
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

        // Following bytes - current cuff pressure
        val cuffPressure: Float = data.getFloat(offset, FloatFormat.IEEE_11073_16_BIT, ByteOrder.LITTLE_ENDIAN)
        offset += 2 + 4 // Skip diastolic and mean arterial pressure, which are set to NaN.

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

        return IntermediateCuffPressureData(cuffPressure, unit, pulseRate, userId, status, calendar)
    }
}