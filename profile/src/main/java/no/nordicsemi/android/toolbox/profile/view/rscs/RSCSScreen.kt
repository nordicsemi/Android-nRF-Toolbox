package no.nordicsemi.android.toolbox.profile.view.rscs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.RSCSServiceData
import no.nordicsemi.android.toolbox.profile.manager.RSCSManager
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCFeatureData
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSData
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSSettingsUnit
import no.nordicsemi.android.toolbox.profile.viewmodel.RSCSEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.RSCSViewModel
import no.nordicsemi.android.ui.view.FeaturesColumn
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.KeyValueColumnReverse
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SectionRow

@Composable
internal fun RSCSScreen(manager: RSCSManager) {
    val rscsViewModel = hiltViewModel<RSCSViewModel, RSCSViewModel.Factory>(
        key = manager.instanceId,
        creationCallback = { factory -> factory.create(manager) }
    )
    val serviceData by rscsViewModel.state.collectAsStateWithLifecycle()
    val onClickEvent: (RSCSEvent) -> Unit = { rscsViewModel.onEvent(it) }

    Column {
        RSCSView(
            serviceData = serviceData,
            onClickEvent = onClickEvent
        )

        serviceData.feature?.let {
            Spacer(modifier = Modifier.height(16.dp))

            RSCSFeaturesView(data = it)
        }
    }
}

@Composable
private fun RSCSView(
    serviceData: RSCSServiceData,
    onClickEvent: (RSCSEvent) -> Unit
) {
    ScreenSection {
        SectionTitle(
            painter = painterResource(R.drawable.ic_rscs),
            title = if (serviceData.data.running)
                stringResource(R.string.rscs_running)
            else
                stringResource(R.string.rscs_walking),
            menu = {
                RSCSSettingsDropdown(serviceData, onClickEvent)
            }
        )
        SectionRow {
            KeyValueColumn(
                key = stringResource(id = R.string.rscs_cadence),
                value = serviceData.displayPace()
            )
            KeyValueColumnReverse(
                key = stringResource(id = R.string.rscs_activity),
                value = if (serviceData.data.running)
                    "\uD83C\uDFC3 ${serviceData.displayActivity()}" else
                    "\uD83D\uDEB6 ${serviceData.displayActivity()}"
            )
        }
        SectionRow {
            KeyValueColumn(
                key = stringResource(R.string.rscs_speed),
                value = serviceData.displaySpeed() ?: "-"
            )
            KeyValueColumnReverse(
                key = stringResource(id = R.string.rscs_stride_length),
                value = serviceData.displayStrideLength()
            )
        }
        KeyValueColumn(
            key = stringResource(R.string.rscs_distance),
            value = serviceData.data.displayDistance(
                serviceData.unit
            )
        )
    }
}

@Composable
private fun RSCSFeaturesView(
    data: RSCFeatureData
) {
    ScreenSection {
        SectionTitle(
            painter = rememberVectorPainter(Icons.Default.Checklist),
            title = stringResource(R.string.rscs_features),
        )
        FeaturesColumn {
            FeatureRow(
                text = stringResource(id = R.string.instantaneous_stride_length_measurement),
                supported = data.instantaneousStrideLengthMeasurementSupported
            )
            FeatureRow(
                text = stringResource(id = R.string.total_distance_measurement),
                supported = data.totalDistanceMeasurementSupported
            )
            FeatureRow(
                text = stringResource(id = R.string.walking_or_running_status),
                supported = data.walkingOrRunningStatusSupported
            )
            FeatureRow(
                text = stringResource(id = R.string.calibration),
                supported = data.calibrationSupported
            )
            FeatureRow(
                text = stringResource(id = R.string.multiple_sensor_location),
                supported = data.multipleSensorLocationsSupported
            )
        }
    }
}

@Composable
private fun RSCSSettingsDropdown(
    state: RSCSServiceData,
    onClickEvent: (RSCSEvent) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    IconButton(
        onClick = { showDialog = true }
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Display settings",
        )
    }

    if (showDialog) {
        RSCSSettingsDialog(
            state = state,
            onDismiss = { showDialog = false },
            onSpeedUnitSelected = onClickEvent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RSCSSettingsDialog(
    state: RSCSServiceData,
    onDismiss: () -> Unit,
    onSpeedUnitSelected: (RSCSEvent) -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(
                text = stringResource(R.string.rscs_settings_unit_title),
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            RSCSSettingsUnit.entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onSpeedUnitSelected(
                                RSCSEvent.OnSelectedSpeedUnitSelected(entry)
                            )
                            onDismiss()
                        }
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = when (state.unit) {
                            entry -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
            }
            // So that bottom padding is 24.dp.
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview
@Composable
private fun RSCSViewPreview() {
    RSCSView(
        serviceData = RSCSServiceData(
            data = RSCSData(
                running = true,
                instantaneousSpeed = 12f,
                instantaneousCadence = 123,
                strideLength = 1234,
                totalDistance = 12345,
            ),
            unit = RSCSSettingsUnit.UNIT_IMPERIAL,
        ),
        onClickEvent = {}
    )
}

@Preview
@Composable
private fun RSCSFeaturesViewPreview() {
    RSCSFeaturesView(
        data = RSCFeatureData(
            instantaneousStrideLengthMeasurementSupported = true,
            totalDistanceMeasurementSupported = true,
            walkingOrRunningStatusSupported = true,
            calibrationSupported = false,
            multipleSensorLocationsSupported = true,
        )
    )
}

@Preview
@Composable
private fun RSCSSettingsDialogPreview() {
    RSCSSettingsDialog(
        state = RSCSServiceData(),
        onDismiss = {},
        onSpeedUnitSelected = {}
    )
}
