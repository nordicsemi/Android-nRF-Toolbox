package no.nordicsemi.android.toolbox.profile.view.bps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.parser.bps.BPMStatus
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureFeatureData
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureMeasurementParser
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureType
import no.nordicsemi.android.toolbox.profile.parser.bps.IntermediateCuffPressureData
import no.nordicsemi.android.toolbox.profile.parser.bps.IntermediateCuffPressureParser
import no.nordicsemi.android.toolbox.profile.viewmodel.BPSViewModel
import no.nordicsemi.android.ui.view.FeaturesColumn
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.KeyValueColumnReverse
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SectionRow
import no.nordicsemi.android.ui.view.StatusColumn

@Composable
internal fun BPSScreen() {
    val bpsViewModel = hiltViewModel<BPSViewModel>()
    val serviceData by bpsViewModel.state.collectAsStateWithLifecycle()

    serviceData.intermediateCuffPressure?.let {
        IntermediateBloodPressureView(it)

        Spacer(modifier = Modifier.height(16.dp))
    }

    BloodPressureView(serviceData.bloodPressureMeasurement)

    serviceData.bloodPressureFeature?.let {
        Spacer(modifier = Modifier.height(16.dp))

        BloodPressureFeatureView(it)
    }
}

@Composable
private fun IntermediateBloodPressureView(
    data: IntermediateCuffPressureData
) {
    ScreenSection {
        SectionTitle(
            painter = painterResource(R.drawable.ic_bps),
            title = stringResource(id = R.string.bps_intermediate_cuff_title),
        )
        SectionRow {
            KeyValueColumn(
                key = stringResource(id = R.string.bps_intermediate_cuff_pressure),
                value = data.displayCuffPressure(),
            )
            KeyValueColumnReverse(
                key = stringResource(id = R.string.bps_pulse),
                value = data.displayHeartRate(),
            )
        }

        data.status?.let { status ->
            HorizontalDivider()

            MeasurementStatusView(status)
        }
    }
}

@Composable
private fun BloodPressureView(
    data: BloodPressureMeasurementData?
) {
    ScreenSection {
        SectionTitle(
            painter = painterResource(R.drawable.ic_bps),
            title = stringResource(id = R.string.bps_title),
        )
        if (data != null) {
            BloodPressureView(data)

            data.status?.let { status ->
                HorizontalDivider()

                MeasurementStatusView(status)
            }
        } else {
            WaitingForMeasurementView()
        }
    }
}

@Composable
internal fun ColumnScope.BloodPressureView(state: BloodPressureMeasurementData) {
    SectionRow {
        KeyValueColumn(
            key = stringResource(id = R.string.bps_systolic),
            value = state.displaySystolic()
        )
        KeyValueColumnReverse(
            key = stringResource(id = R.string.bps_diastolic),
            value = state.displayDiastolic()
        )
    }
    SectionRow {
        KeyValueColumn(
            key = stringResource(id = R.string.bps_mean),
            value = state.displayMeanArterialPressure()
        )
        state.pulseRate?.let {
            KeyValueColumnReverse(
                key = stringResource(id = R.string.bps_pulse),
                value = state.displayPulseRate()
            )
        }
    }
    state.calendar?.let {
        stringResource(R.string.bps_timestamp, it)
    }?.let {
        KeyValueColumn(
            key = "Date & Time",
            value = it
        )
    }
}

@Composable
private fun ColumnScope.MeasurementStatusView(status: BPMStatus) {
    Text(
        text = "Measurement status",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary
    )
    StatusColumn {
        StatusRow(
            text = stringResource(id = R.string.bps_status_body_movement_detected),
            isPresent = status.bodyMovementDetected
        )
        StatusRow(
            text = stringResource(id = R.string.bps_status_cuff_too_lose),
            isPresent = status.cuffTooLose
        )
        StatusRow(
            text = stringResource(id = R.string.bps_status_irregular_heart_rate_detected),
            isPresent = status.irregularPulseDetected
        )
        StatusRow(
            text = stringResource(id = R.string.bps_status_pulse_rate_higher_limit),
            isPresent = status.pulseRateExceedsUpperLimit
        )
        StatusRow(
            text = stringResource(id = R.string.bps_status_pulse_rate_lower_limit),
            isPresent = status.pulseRateIsLessThenLowerLimit
        )
        StatusRow(
            text = stringResource(id = R.string.bps_status_improper_measurement_position),
            isPresent = status.improperMeasurementPosition
        )
    }
}

@Composable
internal fun BloodPressureFeatureView(features: BloodPressureFeatureData) {
    ScreenSection {
        SectionTitle(
            painter = rememberVectorPainter(Icons.Default.Checklist),
            title = stringResource(id = R.string.bps_features_title),
        )
        FeaturesColumn {
            FeatureRow(
                text = stringResource(R.string.bps_feature_body_movement_detection),
                supported = features.bodyMovementDetection
            )
            FeatureRow(
                text = stringResource(R.string.bps_feature_cuff_fit_detection),
                supported = features.cuffFitDetection
            )
            FeatureRow(
                text = stringResource(R.string.bps_feature_irregular_heart_rate_detection),
                supported = features.irregularPulseDetection
            )
            FeatureRow(
                text = stringResource(R.string.bps_feature_pulse_rate_range_detection),
                supported = features.pulseRateRangeDetection
            )
            FeatureRow(
                text = stringResource(R.string.bps_feature_measurement_position_detection),
                supported = features.measurementPositionDetection
            )
            FeatureRow(
                text = stringResource(R.string.bps_feature_multiple_bonds),
                supported = features.multipleBonds
            )
            FeatureRow(
                text = stringResource(R.string.bps_feature_e2e_crc),
                supported = features.e2eCrc
            )
            FeatureRow(
                text = stringResource(R.string.bps_feature_user_data),
                supported = features.userData
            )
            FeatureRow(
                text = stringResource(R.string.bps_feature_user_facing_time),
                supported = features.userFacingTime
            )
        }
    }
}

@Composable
private fun WaitingForMeasurementView() {
    Column {
        Text(text = stringResource(id = R.string.no_data_info_title))
        Text(
            text = stringResource(id = R.string.no_data_info),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.6f),
        )
    }
}

@Preview
@Composable
private fun BloodPressureViewPreview_empty() {
    BloodPressureView(data = null)
}

@Preview
@Composable
private fun BloodPressureViewPreview() {
    val data = byteArrayOf(
        0x1f.toByte(),                          // Flags: All fields present
        0x79.toByte(), 0x00.toByte(),           // Systolic: 121
        0x51.toByte(), 0x00.toByte(),           // Diastolic: 81
        0x6a.toByte(), 0x00.toByte(),           // Mean Arterial Pressure: 106
        0xE4.toByte(),                          // Year LSB (2020)
        0x07.toByte(),                          // Year MSB (2020)
        0x05.toByte(),                          // Month: May
        0x15.toByte(),                          // Day: 21
        0x0A.toByte(),                          // Hour: 10
        0x1E.toByte(),                          // Minute: 30
        0x2D.toByte(),                          // Second: 45
        0x48.toByte(), 0x00.toByte(),           // Pulse Rate: 72.0 bpm
        0x01.toByte(),                          // User ID: 1
        0x06.toByte(), 0x00.toByte()            // Measurement Status: Irregular pulse detected
    )

    BloodPressureView(
        data = BloodPressureMeasurementParser.parse(data)!!,
    )
}

@Preview
@Composable
private fun IntermediatePressureViewPreview() {
    val data = byteArrayOf(
        0x1F.toByte(),                          // Flags: All features enabled
        0x51.toByte(), 0x00.toByte(),           // Cuff pressure (81.0 mmHg)
        0x00.toByte(), 0x00.toByte(),           // following bytes - Diastolic and MAP are unused
        0x00.toByte(), 0x00.toByte(),
        0xE4.toByte(),                          // Year LSB (2020)
        0x07.toByte(),                          // Year MSB (2020)
        0x05.toByte(),                          // Month: May
        0x15.toByte(),                          // Day: 21
        0x0A.toByte(),                          // Hour: 10
        0x1E.toByte(),                          // Minute: 30
        0x2D.toByte(),                          // Second: 45
        0x64.toByte(), 0x00.toByte(),           // Pulse rate (100 bpm)
        0x01.toByte(),                          // User ID (1)
        0xFF.toByte(), 0x01.toByte()            // Measurement status
    )

    IntermediateBloodPressureView(
        data = IntermediateCuffPressureParser.parse(data)!!,
    )
}

@Preview
@Composable
private fun BloodPressureFeatureViewPreview() {
    BloodPressureFeatureView(
        features = BloodPressureFeatureData(
            bodyMovementDetection = false,
            cuffFitDetection = true,
            irregularPulseDetection = false,
            pulseRateRangeDetection = true,
            measurementPositionDetection = false,
            multipleBonds = true,
            e2eCrc = true,
            userData = true,
            userFacingTime = false,
        )
    )
}

@Composable
private fun BloodPressureMeasurementData.displaySystolic(): String =
    stringResource(
        id = R.string.bps_blood_pressure,
        systolic, unit.displayUnit()
    )

@Composable
private fun BloodPressureMeasurementData.displayDiastolic(): String =
    stringResource(
        id = R.string.bps_blood_pressure,
        diastolic, unit.displayUnit()
    )

@Composable
private fun BloodPressureMeasurementData.displayMeanArterialPressure(): String =
    stringResource(
        id = R.string.bps_blood_pressure,
        meanArterialPressure, unit.displayUnit()
    )

@Composable
private fun IntermediateCuffPressureData.displayCuffPressure(): String = cuffPressure.toString() + " ${unit.displayUnit()}"

@Composable
private fun IntermediateCuffPressureData.displayHeartRate(): String = pulseRate?.toString() + " bpm"

@Composable
private fun BloodPressureMeasurementData.displayPulseRate(): String = pulseRate?.toString() + " bpm"

@Composable
private fun BloodPressureType.displayUnit(): String =
    if (this == BloodPressureType.UNIT_MMHG) "mmHg" else "kPA"
