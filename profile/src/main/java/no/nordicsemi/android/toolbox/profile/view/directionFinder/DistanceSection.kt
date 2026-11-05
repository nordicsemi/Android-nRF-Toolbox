package no.nordicsemi.android.toolbox.profile.view.directionFinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.DFSServiceData
import no.nordicsemi.android.toolbox.profile.data.SensorData
import no.nordicsemi.android.toolbox.profile.data.SensorValue
import no.nordicsemi.android.toolbox.profile.data.directionFinder.distanceValue
import no.nordicsemi.android.toolbox.profile.data.directionFinder.isDistanceSettingsAvailable
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.QualityIndicator
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.azimuthal.AzimuthMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.MCPDEstimate
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.McpdMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.RTTEstimate
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.RttMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.elevation.ElevationMeasurementData
import no.nordicsemi.android.toolbox.profile.viewmodel.DFSEvent
import no.nordicsemi.android.ui.view.ScreenSection

@Composable
internal fun DistanceSection(
    sensorData: SensorData,
    range: IntRange,
    onClick: (DFSEvent) -> Unit,
) {
    ScreenSection {
        var showDetails by rememberSaveable { mutableStateOf(false) }

        SectionTitle(
            painter = painterResource(R.drawable.ic_distance),
            title = stringResource(id = R.string.distance_section),
            menu = {
                if (sensorData.isDistanceSettingsAvailable()) {
                    IconButton(
                        onClick = { showDetails = !showDetails }
                    ) {
                        Icon(
                            imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                        )
                    }
                }
            }
        )

        val distanceValue = sensorData.distanceValue()!!
        DistanceView(value = distanceValue, range = range)

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.dm_value, distanceValue),
            style = MaterialTheme.typography.titleLarge,
        )

//    Text(
//        text = stringResource(id = R.string.distance_range),
//        style = MaterialTheme.typography.titleSmall
//    )
//    RangeSlider(
//        range = range,
//        onChange = {
//            onClick(DFSEvent.OnRangeChangedEvent(it))
//        }
//    )

        AnimatedVisibility(showDetails) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HorizontalDivider()
                LinearDataView(data = sensorData, range = range)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DistanceSectionPreview() {
    DistanceSection(
        sensorData = SensorData(
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
                            ifft = 10,
                            phaseSlope = 15,
                            rssi = 30,
                            best = 25,
                        )
                    )
                )
            )
        ),
        range = 0..50,
        onClick = {},
    )
}