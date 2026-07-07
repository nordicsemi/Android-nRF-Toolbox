package no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance

import no.nordicsemi.android.toolbox.profile.parser.directionFinder.AddressType
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.QualityIndicator
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder
import kotlin.experimental.and

class DistanceMeasurementDataParser {

    fun parse(data: ByteArray): DistanceMeasurementData? {
        if (data.size < 10) return null

        var offset = 0

        val flags = data[offset]
            .also { offset += 1 }
        val isRTTPresent = flags and 0x01 > 0
        val isMCPDPresent = flags and 0x02 > 0
        require(isRTTPresent || isMCPDPresent) {
            return null
        }

        // Parse quality indicator.
        val qualityIndicator = data.getInt(offset, IntFormat.UINT8)
            .let { QualityIndicator.create(it) }
            .also { offset += 1 }

        // Parse the target Device Address.
        val addressValue = data
            .sliceArray(offset until offset + 6)
            .reversed()
            .joinToString(":") { "%02X".format(it.toInt() and 0xFF) }
            .also { offset += 6 }

        val addressType = data.getInt(offset, IntFormat.UINT8)
            .let { AddressType.create(it) }
            .also { offset += 1 }
        val address = PeripheralBluetoothAddress(addressType, addressValue)

        // Return data. It can be either one of these, as the type is a union\:
        // https://docs.nordicsemi.com/bundle/nrf-apis-latest/page/structbt_ddfs_distance_measurement.html
        if (isRTTPresent) {
            val rtt = RTTEstimate(data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN))
            return RttMeasurementData(qualityIndicator, address, rtt)
        }

        val mcpd = MCPDEstimate(
            data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN).also { offset += 2 },
            data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN).also { offset += 2 },
            data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN).also { offset += 2 },
            data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN).also { offset += 2 },
        )
        return McpdMeasurementData(qualityIndicator, address, mcpd)
    }
}
