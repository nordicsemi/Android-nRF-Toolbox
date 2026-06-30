package no.nordicsemi.android.toolbox.profile.view.gls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import no.nordicsemi.android.toolbox.profile.data.GLSServiceData
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Carbohydrate
import no.nordicsemi.android.toolbox.profile.parser.gls.data.ConcentrationUnit
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GLSMeasurementContext
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GLSRecord
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GlucoseStatus
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Health
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Meal
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Medication
import no.nordicsemi.android.toolbox.profile.parser.gls.data.MedicationUnit
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RecordType
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus
import no.nordicsemi.android.toolbox.profile.parser.gls.data.SampleLocation
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Tester
import no.nordicsemi.android.toolbox.profile.view.gls.details.GLSDetails
import no.nordicsemi.android.toolbox.profile.viewmodel.GLSEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.GLSEvent.OnWorkingModeSelected
import no.nordicsemi.android.toolbox.profile.viewmodel.GLSViewModel
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.KeyValueColumnReverse
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SectionRow
import java.util.Calendar

@Composable
internal fun GLSScreen() {
    val glsViewModel = hiltViewModel<GLSViewModel>()
    val glsServiceData by glsViewModel.state.collectAsStateWithLifecycle()
    val onClickEvent: (GLSEvent) -> Unit = { glsViewModel.onEvent(it) }

    GLSView(
        data = glsServiceData,
        onClickEvent = onClickEvent,
    )
}

@Composable
private fun GLSView(
    data: GLSServiceData,
    onClickEvent: (GLSEvent) -> Unit
) {
    ScreenSection {
        SectionTitle(
            painter = painterResource(R.drawable.ic_gls),
            title = "Glucose",
            menu = {
                WorkingModeDropDown(
                    data = data,
                    onClickEvent = onClickEvent
                )
            }
        )

        RecordsView(data)
    }
}

@Composable
private fun WorkingModeDropDown(
    data: GLSServiceData,
    onClickEvent: (GLSEvent) -> Unit
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
            glsState = data,
            onDismiss = { showDialog = false },
            onWorkingModeSelected = onClickEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkingModeDialog(
    glsState: GLSServiceData,
    onDismiss: () -> Unit,
    onWorkingModeSelected: (GLSEvent) -> Unit,
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
                text = "Request records",
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
                                OnWorkingModeSelected(entry)
                            )
                            onDismiss()
                        }
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = when (glsState.workingMode) {
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
private fun RecordsView(
    state: GLSServiceData
) {
    if (state.records.isEmpty()) {
        RecordsViewWithoutData()
    } else {
        RecordsViewWithData(state)
    }
}

@Composable
private fun RecordsViewWithoutData() {
    Column {
        Text(text = stringResource(id = R.string.gls_no_records_info))
        Text(
            text = stringResource(id = R.string.gls_no_records_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun RecordsViewWithData(
    state: GLSServiceData
) {
    // Max height for the scrollable section, adjust as needed (e.g. 300.dp)
    Column(
        modifier = Modifier
            .heightIn(max = 500.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.records.keys.forEachIndexed { i, it ->
            RecordItem(it, state.records[it])

            if (i < state.records.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecordItem(
    record: GLSRecord,
    gleContext: GLSMeasurementContext?
) {
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    SectionRow(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { showBottomSheet = true }
            .padding(8.dp)
    ) {
        record.glucoseConcentration?.let { glucoseConcentration ->
            record.unit?.let { unit ->
                glucoseConcentrationDisplayValue(glucoseConcentration, unit)
            }
        }?.let {
            KeyValueColumn(
                key = record.type.toDisplayString(),
                value = it,
            )
        }
        record.time?.let {
            KeyValueColumnReverse(
                key = stringResource(id = R.string.gls_details_date_and_time),
                value = stringResource(R.string.gls_timestamp, it)
            )
        }
    }

    if (showBottomSheet) {
        GLSDetailsBottomSheet(
            record = record,
            context = gleContext,
            onDismiss = { showBottomSheet = false },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GLSDetailsBottomSheet(
    record: GLSRecord,
    context: GLSMeasurementContext?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 16.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .width(50.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    ) {
        GLSDetails(record, context)
    }
}

@Preview
@Composable
private fun GLSViewPreview_empty() {
    GLSView(
        data = GLSServiceData(),
        onClickEvent = {}
    )
}

@Preview
@Composable
private fun GLSViewPreview() {
    GLSView(
        data = GLSServiceData(
            records = mapOf(
                GLSRecord(
                    sequenceNumber = 1,
                    time = Calendar.getInstance(),
                    glucoseConcentration = 0.5f,
                    unit = ConcentrationUnit.UNIT_KGPL,
                    type = RecordType.VENOUS_PLASMA,
                    status = GlucoseStatus(0x03),
                    sampleLocation = SampleLocation.FINGER,
                    contextInformationFollows = true
                ) to GLSMeasurementContext(
                    sequenceNumber = 20,
                    carbohydrate = Carbohydrate.LUNCH,
                    carbohydrateAmount = 12.5f,
                    meal = Meal.CASUAL,
                    tester = Tester.SELF,
                    health = Health.NO_HEALTH_ISSUES,
                    exerciseDuration = 2,
                    exerciseIntensity = 1,
                    medication = Medication.PRE_MIXED_INSULIN,
                    medicationQuantity = .5f,
                    medicationUnit = MedicationUnit.UNIT_KG,
                    HbA1c = 0.5f
                ),
                GLSRecord(
                    sequenceNumber = 2,
                    time = Calendar.getInstance(),
                    glucoseConcentration = 0.6f,
                    unit = ConcentrationUnit.UNIT_MOLPL,
                    type = RecordType.ARTERIAL_PLASMA,
                    status = null,
                    sampleLocation = null,
                    contextInformationFollows = false,
                ) to null,
            )
        ),
        onClickEvent = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun WorkingModeDialogPreview() {
    WorkingModeDialog(
        glsState = GLSServiceData(workingMode = WorkingMode.ALL),
        onDismiss = {},
        onWorkingModeSelected = {}
    )
}
