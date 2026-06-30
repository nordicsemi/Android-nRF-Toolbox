package no.nordicsemi.android.toolbox.profile.view.cscs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import no.nordicsemi.android.toolbox.profile.data.CSCServiceData
import no.nordicsemi.android.toolbox.profile.parser.csc.CSCData
import no.nordicsemi.android.toolbox.profile.parser.csc.SpeedUnit
import no.nordicsemi.android.toolbox.profile.parser.csc.WheelSizes
import no.nordicsemi.android.toolbox.profile.parser.csc.WheelSizes.getWheelSizeByName
import no.nordicsemi.android.toolbox.profile.viewmodel.CSCEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.CSCViewModel
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.KeyValueColumnReverse
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SectionRow

@Composable
internal fun CSCScreen() {
    val csVM = hiltViewModel<CSCViewModel>()
    val onClickEvent: (CSCEvent) -> Unit = { csVM.onEvent(it) }
    val serviceData by csVM.state.collectAsStateWithLifecycle()

    CSCView(
        serviceData = serviceData,
        onClickEvent = onClickEvent
    )
}

@Composable
private fun CSCView(
    serviceData: CSCServiceData,
    onClickEvent: (CSCEvent) -> Unit
) {
    ScreenSection {
        SectionTitle(
            painter = painterResource(R.drawable.ic_csc),
            title = stringResource(R.string.csc_cycling),
            menu = {
                WheelSizeDropDown(
                    state = serviceData,
                    onClickEvent = onClickEvent,
                )
                CSCSettingView(
                    serviceData = serviceData,
                    onClickEvent = onClickEvent,
                )
            }
        )

        SensorsReadingView(state = serviceData, serviceData.speedUnit)
    }
}

@Composable
private fun CSCSettingView(
    serviceData: CSCServiceData,
    onClickEvent: (CSCEvent) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    IconButton(
        onClick = { showDialog = true }
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Speed unit settings",
        )
    }

    if (showDialog) {
        CSCSpeedSettingsFilterDropdown(
            state = serviceData,
            onDismiss = { showDialog = false },
            onClickEvent = onClickEvent,
        )
    }
}

@Composable
private fun WheelSizeDropDown(
    state: CSCServiceData,
    onClickEvent: (CSCEvent) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, start = 24.dp, end = 12.dp),
    ) {
        Text(text = state.data.wheelSize.name)
        Icon(
            modifier = Modifier.padding(start = 8.dp),
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null
        )
    }
    if (expanded) {
        WheelSizeDialog(
            state = state,
            onDismiss = { expanded = false },
            onWheelSizeSelected = { size ->
                onClickEvent(CSCEvent.OnWheelSizeSelected(getWheelSizeByName(size)))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WheelSizeDialog(
    state: CSCServiceData,
    onDismiss: () -> Unit,
    onWheelSizeSelected: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val wheelSizeEntries = WheelSizes.data

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
                text = stringResource(R.string.csc_dialog_title),
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                contentPadding = PaddingValues(bottom = 16.dp), // 24 in total
                state = listState,
            ) {
                items(wheelSizeEntries.size) { index ->
                    val entry = wheelSizeEntries[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onWheelSizeSelected(entry.name)
                                onDismiss()
                            }
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val color = when (state.data.wheelSize) {
                            entry -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                            color = color
                        )
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CSCSpeedSettingsFilterDropdown(
    state: CSCServiceData,
    onDismiss: () -> Unit,
    onClickEvent: (CSCEvent) -> Unit
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
                text = stringResource(R.string.csc_settings),
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            SpeedUnit.entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onClickEvent(CSCEvent.OnSelectedSpeedUnitSelected(entry))
                            onDismiss()
                        }
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                        color = when (state.speedUnit) {
                            entry -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                    )
                    Text(
                        text = entry.unit,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // So that bottom padding is 24.dp.
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ColumnScope.SensorsReadingView(state: CSCServiceData, speedUnit: SpeedUnit) {
    val csc = state.data

    SectionRow {
        KeyValueColumn(
            key = stringResource(id = R.string.csc_field_speed),
            value = csc.displaySpeed(speedUnit)
        )
        KeyValueColumnReverse(
            key = stringResource(id = R.string.csc_field_cadence),
            value = csc.displayCadence()
        )
    }
    SectionRow {
        KeyValueColumn(
            key = stringResource(id = R.string.csc_field_distance),
            value = csc.displayDistance(speedUnit)
        )
        KeyValueColumnReverse(
            key = stringResource(id = R.string.csc_field_total_distance),
            value = csc.displayTotalDistance(speedUnit)
        )
    }
    KeyValueColumn(
        key = stringResource(id = R.string.csc_field_gear_ratio),
        value = csc.displayGearRatio()
    )
}

@Preview(showBackground = true)
@Composable
private fun SensorsReadingViewPreview() {
    CSCView(
        serviceData = CSCServiceData(
            data = CSCData(
                speed = 3.1f,
                cadence = 123f,
                distance = 1234f,
                totalDistance = 12345f,
                gearRatio = 12.3f,
                wheelSize = WheelSizes.data.first()
            )
        ),
        onClickEvent = { }
    )
}

@Preview(showBackground = true)
@Composable
private fun CSCSpeedSettingsFilterDropdownPreview() {
    CSCSpeedSettingsFilterDropdown(
        state = CSCServiceData(),
        onDismiss = {},
        onClickEvent = {}
    )
}
