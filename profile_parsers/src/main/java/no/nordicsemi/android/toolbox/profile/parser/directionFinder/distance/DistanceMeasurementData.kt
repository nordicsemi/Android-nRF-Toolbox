package no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance

import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.QualityIndicator

sealed class DistanceMeasurementData(
    open val quality: QualityIndicator,
    open val address: PeripheralBluetoothAddress
)

data class McpdMeasurementData(
    override val quality: QualityIndicator,
    override val address: PeripheralBluetoothAddress,
    val mcpd: MCPDEstimate
) : DistanceMeasurementData(quality, address)

data class RttMeasurementData(
    override val quality: QualityIndicator,
    override val address: PeripheralBluetoothAddress,
    val rtt: RTTEstimate = RTTEstimate()
) : DistanceMeasurementData(quality, address)

data class MCPDEstimate(
    val ifft: Int = 0,
    val phaseSlope: Int = 0,
    val rssi: Int = 0,
    val best: Int = 0
) {
    operator fun plus(value: Int): MCPDEstimate = MCPDEstimate(
        ifft + value,
        phaseSlope + value,
        rssi + value,
        best + value
    )
}

data class RTTEstimate(
    val value: Int = 0
) {
    operator fun inc(): RTTEstimate = RTTEstimate(value + 1)

    operator fun plus(value: Int): RTTEstimate = RTTEstimate(this.value + value)
}
