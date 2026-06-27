package no.nordicsemi.android.toolbox.profile.parser.directionFinder.elevation

import no.nordicsemi.android.toolbox.profile.parser.directionFinder.AddressType
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.QualityIndicator
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt

class ElevationMeasurementDataParser {

    fun parse(data: ByteArray): ElevationMeasurementData? {
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

        val elevation = data.getInt(offset, IntFormat.INT8)

        return ElevationMeasurementData(qualityIndicator, address, elevation)
    }
}
