package no.nordicsemi.android.toolbox.profile.view.throughput

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.ActionOutlinedButton
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.NumberOfBytes
import no.nordicsemi.android.toolbox.profile.data.NumberOfSeconds
import no.nordicsemi.android.toolbox.profile.data.ThroughputServiceData
import no.nordicsemi.android.toolbox.profile.data.WritingStatus
import no.nordicsemi.android.toolbox.profile.manager.ThroughputManager
import no.nordicsemi.android.toolbox.profile.viewmodel.ThroughputEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.ThroughputViewModel
import no.nordicsemi.android.ui.view.AnimatedThreeDots
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.KeyValueColumnReverse
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SectionRow
import no.nordicsemi.android.ui.view.TextInputField

@Composable
internal fun ThroughputScreen(
    manager: ThroughputManager,
    maxWriteValueLength: Int?,
) {
    val throughputViewModel = hiltViewModel<ThroughputViewModel, ThroughputViewModel.Factory>(
        key = manager.instanceId,
        creationCallback = { factory -> factory.create(manager) }
    )
    val serviceData by throughputViewModel.state.collectAsStateWithLifecycle()
    val onClickEvent: (ThroughputEvent) -> Unit = { throughputViewModel.onEvent(it) }

    // Update the max write value length in the ViewModel.
    LaunchedEffect(maxWriteValueLength != null) {
        onClickEvent(ThroughputEvent.UpdateMaxWriteValueLength(maxWriteValueLength))
    }

    ThroughputContent(serviceData, onClickEvent)
}

@Composable
private fun ThroughputContent(
    serviceData: ThroughputServiceData,
    onClickEvent: (ThroughputEvent) -> Unit
) {
    ScreenSection {
        SectionTitle(
            icon = Icons.Default.SyncAlt,
            title = stringResource(id = R.string.throughput_service_name),
            menu = {
                WorkingModeDropDown(
                    data = serviceData,
                    onClickEvent = onClickEvent,
                )
            },
        )
        // Show throughput data.
        when (serviceData.writingStatus) {
            WritingStatus.IN_PROGRESS ->
                ThroughputInProgress(serviceData.maxWriteValueLength) {
                    AnimatedThreeDots()
                }
            else -> ThroughputData(serviceData)
        }
    }
}

@Composable
private fun WorkingModeDropDown(
    data: ThroughputServiceData,
    onClickEvent: (ThroughputEvent) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    // The Box is required to anchor the drop-down popup.
    Box {
        ActionOutlinedButton(
            text = stringResource(R.string.throughput_write),
            icon = Icons.Filled.PlayArrow,
            onClick = { expanded = true },
            isInProgress = data.writingStatus == WritingStatus.IN_PROGRESS,
        )
        if (expanded) {
            WriteDropdown(
                expanded = expanded,
                onDismiss = { expanded = false },
                onClickEvent = onClickEvent
            )
        }
    }
}

@Composable
fun ThroughputInProgress(
    maxWriteValueLength: Int?,
    animatedThreeDots: @Composable () -> Unit
) {
    SectionRow {
        KeyValueColumn(
            key = stringResource(id = R.string.total_bytes_received),
        ) { animatedThreeDots() }
        KeyValueColumnReverse(
            key = stringResource(id = R.string.gatt_write_number)
        ) { animatedThreeDots() }
    }
    SectionRow {
        KeyValueColumn(
            key = stringResource(id = R.string.measured_throughput)
        ) { animatedThreeDots() }
        // Show mtu size
        maxWriteValueLength?.let { mtu ->
            KeyValueColumnReverse(
                key = stringResource(id = R.string.max_write_value),
                value = "${mtu + 3}"
            )
        }
    }
}

@Composable
private fun ThroughputData(serviceData: ThroughputServiceData) {
    serviceData.throughputData.let {
        SectionRow {
            KeyValueColumn(
                key = stringResource(id = R.string.total_bytes_received),
                value = it.throughputDataReceived()
            )
            KeyValueColumnReverse(
                key = stringResource(id = R.string.gatt_write_number),
                value = it.gattWritesReceived.toString()
            )
        }
        SectionRow {
            KeyValueColumn(
                key = stringResource(id = R.string.measured_throughput),
                value = it.displayThroughput()
            )
            // Show mtu size
            serviceData.maxWriteValueLength?.let { mtu ->
                KeyValueColumnReverse(
                    key = stringResource(id = R.string.max_write_value),
                    value = "${mtu + 3}"
                )
            }
        }
    }
}

@Composable
private fun WriteDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onClickEvent: (ThroughputEvent) -> Unit
) {
    var number by rememberSaveable { mutableIntStateOf(0) }
    var writeDataType by rememberSaveable { mutableStateOf("") }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(8.dp)
    ) {
        when (writeDataType) {
            NumberOfBytes.getString() -> {
                // Show bytes input
                TextInputField(
                    input = number.toString(),
                    label = stringResource(id = R.string.throughput_bytes),
                    placeholder = stringResource(id = R.string.throughput_bytes_description),
                    errorState = number < 0,
                    errorMessage = stringResource(id = R.string.throughput_bytes_error),
                    onUpdate = { number = it.toIntOrNull() ?: 0 },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }

            NumberOfSeconds.getString() -> {
                // Show time input
                TextInputField(
                    input = number.toString(),
                    label = stringResource(id = R.string.throughput_time),
                    placeholder = stringResource(id = R.string.throughput_time_description),
                    errorState = number < 0,
                    errorMessage = stringResource(id = R.string.throughput_time_error),
                    onUpdate = {number = it.toIntOrNull() ?: 0 },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }

            else -> {
                // Show throughput input type
                getThroughputInputTypes().forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            writeDataType = it
                            when (it) {
                                NumberOfBytes.getString() -> number = 100
                                NumberOfSeconds.getString() -> number = 20
                            }
                        }
                    )
                }
            }
        }
        // Run button.
        if (writeDataType.isNotEmpty() && number > 0) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = {
                        onClickEvent(
                            ThroughputEvent.OnWriteData(
                                when (writeDataType) {
                                    NumberOfBytes.getString() -> NumberOfBytes(number * 1024)
                                    NumberOfSeconds.getString() -> NumberOfSeconds(number)
                                    else -> throw IllegalArgumentException("Invalid throughput input type")
                                }
                            )
                        )
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.throughput_start))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ThroughputScreenPreview() {
    ThroughputContent(
        serviceData = ThroughputServiceData(),
        onClickEvent = {}
    )
}
