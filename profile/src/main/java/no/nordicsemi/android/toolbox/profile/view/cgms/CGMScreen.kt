package no.nordicsemi.android.toolbox.profile.view.cgms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
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
import no.nordicsemi.android.common.ui.view.ActionOutlinedButton
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.CGMRecordWithSequenceNumber
import no.nordicsemi.android.toolbox.profile.data.CGMServiceData
import no.nordicsemi.android.toolbox.profile.parser.cgms.data.CGMRecord
import no.nordicsemi.android.toolbox.profile.parser.cgms.data.CGMStatus
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus
import no.nordicsemi.android.toolbox.profile.viewmodel.CGMSEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.CGMSViewModel
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.KeyValueColumnReverse
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SectionRow
import java.util.Calendar

@Composable
internal fun CGMScreen() {
    val cgmVm = hiltViewModel<CGMSViewModel>()
    val serviceData by cgmVm.state.collectAsStateWithLifecycle()
    val onClickEvent: (CGMSEvent) -> Unit = { cgmVm.onEvent(it) }

    CGMSView(serviceData, onClickEvent)
}

@Composable
private fun CGMSView(
    serviceData: CGMServiceData,
    onClickEvent: (CGMSEvent) -> Unit,
) {
    ScreenSection {
        SectionTitle(
            painter = painterResource(R.drawable.ic_cgm),
            title = stringResource(R.string.cgms_title),
            menu = {
                WorkingModeDropDown(
                    data = serviceData,
                    onClickEvent = onClickEvent,
                )
            }
        )

        RecordsView(serviceData)
    }
}

@Composable
private fun WorkingModeDropDown(
    data: CGMServiceData,
    onClickEvent: (CGMSEvent) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    ActionOutlinedButton(
        text = "Request",
        icon = Icons.Default.Download,
        onClick = { showDialog = true },
        isInProgress = data.requestStatus == RequestStatus.PENDING,
    )
    if (showDialog) {
        WorkingModeDialog(
            cgmState = data,
            onDismiss = { showDialog = false },
            onWorkingModeSelected = onClickEvent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkingModeDialog(
    cgmState: CGMServiceData,
    onDismiss: () -> Unit,
    onWorkingModeSelected: (CGMSEvent) -> Unit,
) {
    val workingModeEntries = WorkingMode.entries.toList()

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
                text = "Request record",
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            workingModeEntries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onWorkingModeSelected(
                                CGMSEvent.OnWorkingModeSelected(entry)
                            )
                            onDismiss()
                        }
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = when (cgmState.workingMode) {
                        entry -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onBackground
                    }
                    Text(
                        text = entry.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                        color = color
                    )
                }
            }
            // So that bottom padding is 24.dp.
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RecordsView(state: CGMServiceData) {
    if (state.records.isEmpty()) {
        RecordsViewWithoutData()
    } else {
        RecordsViewWithData(state)
    }
}

@Composable
private fun RecordsViewWithoutData() {
    Column {
        Text(text = stringResource(id = R.string.cgms_no_records_info))
        Text(
            text = stringResource(id = R.string.cgms_no_records_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun RecordsViewWithData(state: CGMServiceData) {
    val newRecord = when (state.workingMode) {
        WorkingMode.ALL -> state.records
        WorkingMode.LAST -> listOf(state.records.last())
        WorkingMode.FIRST -> listOf(state.records.first())
        null -> state.records
    }

    // Max height for the scrollable section, adjust as needed (e.g. 300.dp)
    Column(
        modifier = Modifier
            .heightIn(max = 500.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        newRecord.forEachIndexed { i, it ->
            RecordItem(it)

            if (i < newRecord.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ColumnScope.RecordItem(record: CGMRecordWithSequenceNumber) {
    SectionRow {
        KeyValueColumn(
            key = stringResource(id = R.string.cgms_sequence_number),
            value = record.sequenceNumber.toString()
        )
        KeyValueColumnReverse(
            key = "Glucose concentration",
            value = record.glucoseConcentration(),
        )
    }
    KeyValueColumn(
        key = "Date & Time",
        value = record.formattedTime()
    )
}

@Preview
@Composable
private fun CGMSViewPreview() {
    CGMSView(
        serviceData = CGMServiceData(
            records = listOf(
                CGMRecordWithSequenceNumber(
                    sequenceNumber = 12,
                    record = CGMRecord(
                        glucoseConcentration = 0.5f,
                        trend = 2.05f,
                        quality = 0.5f,
                        status = CGMStatus(2, 3, 5),
                        timeOffset = 2,
                        crcPresent = true
                    ),
                    timestamp = Calendar.TUESDAY.toLong()
                ),
                CGMRecordWithSequenceNumber(
                    sequenceNumber = 13,
                    record = CGMRecord(
                        glucoseConcentration = 0.5f,
                        trend = 2.05f,
                        quality = 0.5f,
                        status = CGMStatus(2, 3, 5),
                        timeOffset = 2,
                        crcPresent = true
                    ),
                    timestamp = Calendar.TUESDAY.toLong()
                ),
            ),
        ),
        onClickEvent = {}
    )
}

@Preview
@Composable
private fun CGMSViewPreview_empty() {
    CGMSView(
        serviceData = CGMServiceData(
            records = emptyList(),
        ),
        onClickEvent = {}
    )
}
