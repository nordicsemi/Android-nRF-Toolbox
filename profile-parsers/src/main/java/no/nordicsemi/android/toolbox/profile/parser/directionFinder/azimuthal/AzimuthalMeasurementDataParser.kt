package no.nordicsemi.android.toolbox.profile.parser.directionFinder.azimuthal

import no.nordicsemi.android.toolbox.profile.parser.directionFinder.AddressType
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.QualityIndicator
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder

class AzimuthalMeasurementDataParser {

    fun parse(
        data: ByteArray,
        byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): AzimuthMeasurementData? {
        if (data.size < 10) return null

        var offset = 1 // Skip flags

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

        val azimuth = data.getInt(offset, IntFormat.UINT16, byteOrder)

        return AzimuthMeasurementData(qualityIndicator, address, azimuth)

    }
}
