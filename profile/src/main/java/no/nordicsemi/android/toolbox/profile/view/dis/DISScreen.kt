package no.nordicsemi.android.toolbox.profile.view.dis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.DISServiceData
import no.nordicsemi.android.toolbox.profile.manager.DISManager
import no.nordicsemi.android.toolbox.profile.viewmodel.DISViewModel
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.ScreenSection

@Composable
internal fun DISScreen(manager: DISManager) {
    val disViewModel = hiltViewModel<DISViewModel, DISViewModel.Factory>(
        key = manager.instanceId,
        creationCallback = { factory -> factory.create(manager) }
    )
    val disServiceData by disViewModel.state.collectAsStateWithLifecycle()

    DISView(disServiceData)
}

@Composable
private fun DISView(data: DISServiceData) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ScreenSection(
        modifier = Modifier.clickable { expanded = !expanded },
    ) {
        SectionTitle(
            icon = Icons.Default.Info,
            title = stringResource(id = R.string.dis_label),
            menu = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
        )

        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                data.manufacturerName?.let {
                    KeyValueColumn(key = stringResource(R.string.dis_manufacturer_name), value = it)
                }
                data.modelNumber?.let {
                    KeyValueColumn(key = stringResource(R.string.dis_model_number), value = it)
                }
                data.serialNumber?.let {
                    KeyValueColumn(key = stringResource(R.string.dis_serial_number), value = it)
                }
                data.hardwareRevision?.let {
                    KeyValueColumn(key = stringResource(R.string.dis_hardware_revision), value = it)
                }
                data.firmwareRevision?.let {
                    KeyValueColumn(key = stringResource(R.string.dis_firmware_revision), value = it)
                }
                data.softwareRevision?.let {
                    KeyValueColumn(key = stringResource(R.string.dis_software_revision), value = it)
                }
                data.systemId?.let {
                    KeyValueColumn(key = stringResource(R.string.dis_system_id), value = it)
                }
                data.ieeeCertificationData?.let {
                    KeyValueColumn(key = stringResource(R.string.dis_ieee_certification_data), value = it)
                }
            }
        }
    }
}

@Preview
@Composable
private fun DISPreview() {
    DISView(
        DISServiceData(
            manufacturerName = "Nordic Semiconductor ASA",
            modelNumber = "nRF52840 DK",
            firmwareRevision = "1.0.0+3",
            systemId = "00:11:22:33:44:55:66:77",
        )
    )
}
