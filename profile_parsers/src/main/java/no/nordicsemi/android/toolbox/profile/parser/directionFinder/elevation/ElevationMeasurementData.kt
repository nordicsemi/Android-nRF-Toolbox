package no.nordicsemi.android.toolbox.profile.parser.directionFinder.elevation

import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.QualityIndicator

data class ElevationMeasurementData(
    val quality: QualityIndicator,
    val address: PeripheralBluetoothAddress,
    val elevation: Int
)