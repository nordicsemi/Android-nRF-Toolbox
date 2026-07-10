package no.nordicsemi.android.toolbox.profile.view.throughput

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import no.nordicsemi.android.toolbox.profile.parser.throughput.ThroughputMetrics
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
                ThroughputInProgress(serviceData.maxWriteValueLength)
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
) {
    Row(
        modifier = Modifier.height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedThreeDots(
            dotSize = 10.dp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
        )
        AnimatedThreeDots(
            modifier = Modifier.width(48.dp),
            dotSize = 4.dp,
        )
        Icon(
            imageVector = Icons.Default.DeveloperBoard,
            contentDescription = null,
        )
    }
    SectionRow {
        KeyValueColumn(
            key = stringResource(id = R.string.total_bytes_received),
        ) { AnimatedThreeDots(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
        maxWriteValueLength?.let { mtu ->
            KeyValueColumnReverse(
                key = stringResource(id = R.string.max_write_value),
            ) { AnimatedThreeDots(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
        } ?: KeyValueColumnReverse(
            key = stringResource(id = R.string.gatt_write_number),
        ) { AnimatedThreeDots(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
    }
}

@Composable
private fun ThroughputData(serviceData: ThroughputServiceData) {
    serviceData.throughputData.let {
        Row(
            modifier = Modifier.height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = it.displayThroughput(),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.size(size = 8.dp))
            Text(
                text = "kB/s",
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                contentDescription = null,
                modifier = Modifier.width(48.dp),
            )
            Icon(
                imageVector = Icons.Default.DeveloperBoard,
                contentDescription = null,
            )
        }
        SectionRow {
            KeyValueColumn(
                key = stringResource(id = R.string.total_bytes_received),
                value = it.throughputDataReceived()
            )
            serviceData.maxWriteValueLength?.let { length ->
                KeyValueColumnReverse(
                    key = stringResource(id = R.string.max_write_value),
                    value = "${it.gattWritesReceived} × $length"
                )
            } ?: KeyValueColumnReverse(
                key = stringResource(id = R.string.gatt_write_number),
                value = it.gattWritesReceived.toString()
            )
        }
    }
}

@Composable
private fun WriteDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onClickEvent: (ThroughputEvent) -> Unit
) {
    var number by rememberSaveable { mutableStateOf("") }
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
                    input = number,
                    label = stringResource(id = R.string.throughput_bytes),
                    placeholder = stringResource(id = R.string.throughput_bytes),
                    errorState = number.isNotEmpty() && number.toIntOrNull() == null,
                    errorMessage = stringResource(id = R.string.throughput_bytes_error),
                    onUpdate = { number = it },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }

            NumberOfSeconds.getString() -> {
                // Show time input
                TextInputField(
                    input = number,
                    label = stringResource(id = R.string.throughput_time),
                    placeholder = stringResource(id = R.string.throughput_time),
                    errorState = number.isNotEmpty() && number.toIntOrNull() == null,
                    errorMessage = stringResource(id = R.string.throughput_time_error),
                    onUpdate = { number = it },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }

            else -> {
                // Show throughput input type
                getThroughputInputTypes().forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value) },
                        onClick = {
                            writeDataType = value
                            when (value) {
                                NumberOfBytes.getString() -> number = "500"
                                NumberOfSeconds.getString() -> number = "10"
                            }
                        }
                    )
                }
            }
        }
        // Run button.
        if (writeDataType.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = {
                        val n = number.toIntOrNull() ?: return@Button
                        onClickEvent(
                            ThroughputEvent.OnWriteData(
                                when (writeDataType) {
                                    NumberOfBytes.getString() -> NumberOfBytes(n * 1024)
                                    NumberOfSeconds.getString() -> NumberOfSeconds(n)
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
        serviceData = ThroughputServiceData(
            throughputData = ThroughputMetrics(
                gattWritesReceived = 1527,
                totalBytesReceived = 755865,
                throughputBitsPerSecond = 355331,
            ),
            maxWriteValueLength = 495,
        ),
        onClickEvent = {}
    )
}

@Preview
@Composable
private fun ThroughputScreenPreview_InProgress() {
    ThroughputContent(
        serviceData = ThroughputServiceData(
            writingStatus = WritingStatus.IN_PROGRESS,
            maxWriteValueLength = 495,
        ),
        onClickEvent = {}
    )
}
