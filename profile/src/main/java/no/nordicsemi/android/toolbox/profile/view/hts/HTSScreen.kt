package no.nordicsemi.android.toolbox.profile.view.hts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import no.nordicsemi.android.toolbox.profile.data.HTSServiceData
import no.nordicsemi.android.toolbox.profile.data.uiMapper.TemperatureUnit
import no.nordicsemi.android.toolbox.profile.parser.hts.HTSData
import no.nordicsemi.android.toolbox.profile.parser.hts.HTSMeasurementType
import no.nordicsemi.android.toolbox.profile.viewmodel.HTSEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.HTSViewModel
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.KeyValueColumnReverse
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SectionRow
import no.nordicsemi.android.ui.view.TextWithAnimatedDots
import java.util.Calendar

@Composable
internal fun HTSScreen() {
    val htsViewModel = hiltViewModel<HTSViewModel>()
    val onClickEvent: (HTSEvent) -> Unit = { htsViewModel.onEvent(it) }
    val htsServiceData by htsViewModel.htsServiceState.collectAsStateWithLifecycle()

    HTSContent(htsServiceData, onClickEvent)
}

@Composable
private fun HTSContent(
    htsServiceData: HTSServiceData,
    onClickEvent: (HTSEvent) -> Unit
) {
    ScreenSection {
        SectionTitle(
            painter = painterResource(R.drawable.ic_hts),
            title = stringResource(id = R.string.hts_temperature),
            menu = {
                TemperatureUnitSettings(
                    state = htsServiceData,
                    onClickEvent = { onClickEvent(it) },
                )
            }
        )
        SectionRow(
            verticalAlignment = Alignment.Top,
        ) {
            htsServiceData.data?.temperature?.let { temperature ->
                KeyValueColumn(
                    key = stringResource(id = R.string.temperature_title),
                    value = htsServiceData.temperatureUnit.displayTemperature(temperature),
                    valueStyle = MaterialTheme.typography.displayMedium,
                )
            } ?: run {
                TextWithAnimatedDots(text = stringResource(id = R.string.reading_temperature_placeholder))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                htsServiceData.data?.let { data ->
                    KeyValueColumnReverse(
                        key = stringResource(id = R.string.temp_measurement_location),
                        value = data.type?.toString() ?: "Unknown",
                    )
                }
                htsServiceData.data?.timestamp?.let {
                    KeyValueColumnReverse(
                        key = stringResource(R.string.temp_measurement_time),
                        value = it.toFormattedString(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TemperatureUnitSettings(
    state: HTSServiceData,
    onClickEvent: (HTSEvent) -> Unit
) {
    var openSettingsDialog by rememberSaveable { mutableStateOf(false) }

    Icon(
        imageVector = Icons.Outlined.Settings,
        contentDescription = stringResource(id = R.string.hts_temperature_unit_des),
        modifier = Modifier
            .clip(CircleShape)
            .clickable { openSettingsDialog = true }
    )
    if (openSettingsDialog) {
        TemperatureUnitSettingsDialog(
            state = state,
            onDismiss = { openSettingsDialog = false },
            onClickEvent = onClickEvent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemperatureUnitSettingsDialog(
    state: HTSServiceData,
    onDismiss: () -> Unit,
    onClickEvent: (HTSEvent) -> Unit,
) {
    val entries = TemperatureUnit.entries.toList()

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
                text = stringResource(R.string.hts_temperature_unit),
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClickEvent(
                                HTSEvent.OnTemperatureUnitSelected(entry)
                            )
                            onDismiss()
                        },
                ) {
                    Text(
                        text = entry.toString(),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        color = when (state.temperatureUnit) {
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
private fun HTSContentPreview_reading() {
    HTSContent(
        htsServiceData = HTSServiceData(
            data = null,
            temperatureUnit = TemperatureUnit.CELSIUS
        ),
        onClickEvent = {}
    )
}

@Preview
@Composable
private fun HTSContentPreview() {
    HTSContent(
        htsServiceData = HTSServiceData(
            data = HTSData(
                temperature = 36.5f,
                type = HTSMeasurementType.FINGER,
                timestamp = Calendar.getInstance()
            ),
            temperatureUnit = TemperatureUnit.CELSIUS
        ),
        onClickEvent = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun TemperatureUnitSettingsDialogPreview() {
    TemperatureUnitSettingsDialog(
        state = HTSServiceData(),
        onDismiss = {},
        onClickEvent = {}
    )
}