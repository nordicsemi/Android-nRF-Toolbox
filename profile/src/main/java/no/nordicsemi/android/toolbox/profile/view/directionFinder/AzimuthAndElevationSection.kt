package no.nordicsemi.android.toolbox.profile.view.directionFinder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.azimuthal.AzimuthMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.elevation.ElevationMeasurementData
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.SensorData
import no.nordicsemi.android.toolbox.profile.data.SensorValue
import no.nordicsemi.android.toolbox.profile.data.directionFinder.displayAzimuth
import no.nordicsemi.android.toolbox.profile.data.directionFinder.elevationValue
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.QualityIndicator
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SubsectionTitle

@Composable
internal fun AzimuthAndElevationSection(data: SensorData, range: IntRange) {
    ScreenSection {
        data.displayAzimuth()?.let {
            SectionTitle(
                painter = painterResource(id = R.drawable.ic_azimuth),
                title = stringResource(id = R.string.azimuth_section)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AzimuthView(data, range)
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        data.elevationValue()?.let {
            SectionTitle(
                painter = painterResource(id = R.drawable.ic_elevation),
                title = stringResource(id = R.string.elevation_section)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ElevationView(value = it)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AzimuthAndElevationSectionPreview() {
    val sensorData = SensorData(
        azimuth = SensorValue(
            values = listOf(
                AzimuthMeasurementData(
                    quality = QualityIndicator.POOR,
                    address = PeripheralBluetoothAddress.TEST,
                    azimuth = 50,
                )
            )
        ),
        elevation = SensorValue(
            values = listOf(
                ElevationMeasurementData(
                    quality = QualityIndicator.GOOD,
                    address = PeripheralBluetoothAddress.TEST,
                    elevation = 30
                )
            )
        )
    )
    AzimuthAndElevationSection(sensorData, 0..50)
}