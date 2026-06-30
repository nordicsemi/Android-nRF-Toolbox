package no.nordicsemi.android.toolbox.profile.view.directionFinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.toolbox.profile.data.DFSServiceData
import no.nordicsemi.android.toolbox.profile.data.SensorData
import no.nordicsemi.android.toolbox.profile.data.SensorValue
import no.nordicsemi.android.toolbox.profile.data.directionFinder.distanceValue
import no.nordicsemi.android.toolbox.profile.data.directionFinder.isAzimuthAndElevationDataAvailable
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.QualityIndicator
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.azimuthal.AzimuthMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.MCPDEstimate
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.McpdMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.RTTEstimate
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.RttMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.elevation.ElevationMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus
import no.nordicsemi.android.toolbox.profile.viewmodel.DFSEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.DirectionFinderViewModel

@Composable
internal fun DFSScreen() {
    val dfsVM = hiltViewModel<DirectionFinderViewModel>()
    val onClick: (DFSEvent) -> Unit = { dfsVM.onEvent(it) }
    val serviceData by dfsVM.state.collectAsStateWithLifecycle()

    DFSView(serviceData, onClick)
}

@Composable
private fun DFSView(
    serviceData: DFSServiceData,
    onClick: (DFSEvent) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionBluetoothDeviceComponent(
            data = serviceData,
            selectedDevice = serviceData.selectedDevice,
            onEvent = onClick,
        )

        val data = serviceData.data[serviceData.selectedDevice]
        data?.distanceValue()?.let {
            DistanceSection(data, serviceData.distanceRange, onClick)
        }

        val isAzimuthAndElevationDataAvailable = data?.isAzimuthAndElevationDataAvailable() ?: false
        if (isAzimuthAndElevationDataAvailable) {
            AzimuthAndElevationSection(data, serviceData.distanceRange)
        }
    }
}

@Preview
@Composable
private fun LoadingViewPreview() {
    DFSView(
        serviceData = DFSServiceData(),
        onClick = {}
    )
}

@Preview
@Composable
private fun ScanningPreview() {
    DFSView(
        serviceData = DFSServiceData(
            requestStatus = RequestStatus.SUCCESS
        ),
        onClick = {}
    )
}

@Preview(heightDp = 1600)
@Composable
private fun DFSPreview() {
    DFSView(
        serviceData = DFSServiceData(
            requestStatus = RequestStatus.SUCCESS,
            selectedDevice = PeripheralBluetoothAddress.TEST,
            data = mapOf(
                PeripheralBluetoothAddress.TEST to SensorData(
                    azimuth = SensorValue(
                        values = listOf(
                            AzimuthMeasurementData(
                                quality = QualityIndicator.POOR,
                                address = PeripheralBluetoothAddress.TEST,
                                azimuth = 50,
                            ),
                        )
                    ),
                    elevation = SensorValue(
                        values = listOf(
                            ElevationMeasurementData(
                                quality = QualityIndicator.GOOD,
                                address = PeripheralBluetoothAddress.TEST,
                                elevation = 30,
                            )
                        )
                    ),
                    rttDistance = SensorValue(
                        values = listOf(
                            RttMeasurementData(
                                quality = QualityIndicator.POOR,
                                address = PeripheralBluetoothAddress.TEST,
                                rtt = RTTEstimate(10),
                            )
                        )
                    ),
                    mcpdDistance = SensorValue(
                        values = listOf(
                            McpdMeasurementData(
                                quality = QualityIndicator.POOR,
                                address = PeripheralBluetoothAddress.TEST,
                                mcpd = MCPDEstimate(
                                    ifft = 14,
                                    phaseSlope = 15,
                                    rssi = 16,
                                    best = 17
                                )
                            )
                        )
                    )
                )
            )
        ),
        onClick = {}
    )
}