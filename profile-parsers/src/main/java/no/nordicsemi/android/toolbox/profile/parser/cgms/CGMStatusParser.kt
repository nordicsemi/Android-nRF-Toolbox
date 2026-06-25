package no.nordicsemi.android.toolbox.profile.parser.cgms

import no.nordicsemi.android.toolbox.profile.parser.cgms.data.CGMStatus
import no.nordicsemi.android.toolbox.profile.parser.cgms.data.CGMStatusEnvelope
import no.nordicsemi.android.toolbox.profile.parser.common.CRC16
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder

object CGMStatusParser {

    fun parse(
        data: ByteArray,
        byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): CGMStatusEnvelope? {
        if (data.size != 5 && data.size != 7) return null

        val timeOffset: Int = data.getInt(0, IntFormat.UINT16, byteOrder)
        val warningStatus: Int = data.getInt(2, IntFormat.UINT8)
        val calibrationTempStatus: Int = data.getInt(3, IntFormat.UINT8)
        val sensorStatus: Int = data.getInt(4, IntFormat.UINT8)

        val crcPresent = data.size == 7
        if (crcPresent) {
            val actualCrc: Int = CRC16.MCRF4XX(data, 0, 5)
            val expectedCrc: Int = data.getInt(5, IntFormat.UINT16, byteOrder)
            if (actualCrc != expectedCrc) {
                return null
            }
        }

        val status = CGMStatus(warningStatus, calibrationTempStatus, sensorStatus)
        return CGMStatusEnvelope(status, timeOffset, crcPresent, crcPresent)
    }
}
