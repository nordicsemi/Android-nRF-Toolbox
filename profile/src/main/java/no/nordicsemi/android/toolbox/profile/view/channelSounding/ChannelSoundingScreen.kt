package no.nordicsemi.android.toolbox.profile.view.channelSounding

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.theme.nordicGreen
import no.nordicsemi.android.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.AntennaMode
import no.nordicsemi.android.toolbox.profile.data.CSRangingMeasurement
import no.nordicsemi.android.toolbox.profile.data.CapabilityStatus
import no.nordicsemi.android.toolbox.profile.data.ChannelSoundingServiceData
import no.nordicsemi.android.toolbox.profile.data.ConfidenceLevel
import no.nordicsemi.android.toolbox.profile.data.CsRangingData
import no.nordicsemi.android.toolbox.profile.data.HostCapabilities
import no.nordicsemi.android.toolbox.profile.data.LocationType
import no.nordicsemi.android.toolbox.profile.data.RangingOptions
import no.nordicsemi.android.toolbox.profile.data.RangingSessionAction
import no.nordicsemi.android.toolbox.profile.data.RangingTechnology
import no.nordicsemi.android.toolbox.profile.data.SessionCloseReasonProvider
import no.nordicsemi.android.toolbox.profile.data.SessionClosedReason
import no.nordicsemi.android.toolbox.profile.data.SightType
import no.nordicsemi.android.toolbox.profile.data.TechnologyAvailability
import no.nordicsemi.android.toolbox.profile.data.UpdateRate
import no.nordicsemi.android.toolbox.profile.manager.ChannelSoundingManager
import no.nordicsemi.android.toolbox.profile.viewmodel.ChannelSoundingEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.ChannelSoundingViewModel
import no.nordicsemi.android.ui.view.AnimatedThreeDots
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.internal.LoadingView

@Composable
internal fun ChannelSoundingScreen(
    manager: ChannelSoundingManager,
    isNotificationPermissionGranted: Boolean?,
) {
    // Channel Sounding is available from Android 16 (API 36) onward, while better accuracy and
    // performance are provided from Android 16 (API 36, minor version 1) and later.
    if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1 && isNotificationPermissionGranted != null) {
        RequestRangingPermission {
            val channelSoundingViewModel = hiltViewModel<ChannelSoundingViewModel, ChannelSoundingViewModel.Factory>(
                key = manager.instanceId,
                creationCallback = { factory -> factory.create(manager) }
            )
            val channelSoundingState by channelSoundingViewModel.state.collectAsStateWithLifecycle()

            val onClickEvent: (event: ChannelSoundingEvent) -> Unit = {
                channelSoundingViewModel.onEvent(it)
            }

            ChannelSoundingView(channelSoundingState, onClickEvent)
        }
    } else {
        SessionError(reason = SessionClosedReason.TOO_OLD)
    }
}

@Composable
private fun ChannelSoundingView(
    channelSoundingState: ChannelSoundingServiceData,
    onClickEvent: (ChannelSoundingEvent) -> Unit,
) {
    val action = channelSoundingState.rangingSessionAction

    // Fatal errors replace the whole screen - the user cannot recover by starting a session.
    val fatalReason = (action as? RangingSessionAction.OnError)?.reason?.takeIf { it.isFatal() }
    if (fatalReason != null) {
        SessionError(reason = fatalReason)
        return
    }

    // Host capabilities are fetched once right after connecting; until then there is nothing to show.
    val capabilities = channelSoundingState.capabilities
    if (capabilities == null) {
        LoadingView()
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConfigCard(
            config = channelSoundingState.config,
            isRunning = channelSoundingState.isRunning,
            onClickEvent = onClickEvent,
        )

        (action as? RangingSessionAction.OnError)?.let { InlineError(it.reason) }

        (action as? RangingSessionAction.OnResult)?.let { action ->
            LiveReadoutsCard(action.data)
            RecentMeasurementsChart(action.previousData)
        }

        var expanded by rememberSaveable { mutableStateOf(false) }
        CapabilitiesCard(
            capabilities = capabilities,
            expanded = expanded,
            onExpandedChange = { expanded = it },
        )
    }
}

@Composable
private fun CapabilitiesCard(
    capabilities: HostCapabilities,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    ScreenSection(
        modifier = Modifier.clickable { onExpandedChange(!expanded) },
    ) {
        SectionTitle(
            icon = Icons.Default.VerifiedUser,
            title = stringResource(R.string.host_capabilities),
            menu = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
        )

        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                capabilities.technologies.forEach { availability ->
                    LabeledRow(
                        label = technologyLabel(availability),
                        value = stringResource(availability.status.toUiString()),
                        extra = technologyExtra(availability),
                        valueColor = when (availability.status) {
                            CapabilityStatus.ENABLED -> nordicGreen
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                if (capabilities.supportedSecurityLevels.isNotEmpty()) {
                    HorizontalDivider()
                    LabeledRow(
                        label = stringResource(R.string.cs_security_levels),
                        value = capabilities.supportedSecurityLevels.joinToString(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigCard(
    config: RangingOptions,
    isRunning: Boolean,
    onClickEvent: (ChannelSoundingEvent) -> Unit,
) {
    ScreenSection {
        SectionTitle(
            icon = Icons.Default.SocialDistance,
            title = stringResource(R.string.channel_sounding),
        )

        LabeledRow(
            label = stringResource(R.string.ranging_technology),
            value = stringResource(RangingTechnology.BLE_CS.toUiString()),
        )

        DropdownOptionRow(
            title = stringResource(R.string.update_rate),
            options = UpdateRate.entries,
            selected = config.updateRate,
            labelOf = { stringResource(it.toUiString()) },
            enabled = !isRunning,
            onSelected = { onClickEvent(ChannelSoundingEvent.SetUpdateRate(it)) },
        )

        DropdownOptionRow(
            title = stringResource(R.string.sight_type),
            options = SightType.entries,
            selected = config.sightType,
            labelOf = { stringResource(it.toUiString()) },
            enabled = !isRunning,
            onSelected = { onClickEvent(ChannelSoundingEvent.SetSightType(it)) },
        )

        DropdownOptionRow(
            title = stringResource(R.string.location_type),
            options = LocationType.entries,
            selected = config.locationType,
            labelOf = { stringResource(it.toUiString()) },
            enabled = !isRunning,
            onSelected = { onClickEvent(ChannelSoundingEvent.SetLocationType(it)) },
        )

        // Antenna mode is only configurable from Android 17 (CINNAMON_BUN) onward.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            DropdownOptionRow(
                title = stringResource(R.string.antenna_mode),
                options = AntennaMode.entries,
                selected = config.antennaMode,
                labelOf = { stringResource(it.toUiString()) },
                enabled = !isRunning,
                onSelected = { onClickEvent(ChannelSoundingEvent.SetAntennaMode(it)) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.sensor_fusion),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = config.sensorFusionEnabled,
                enabled = !isRunning,
                onCheckedChange = { onClickEvent(ChannelSoundingEvent.SetSensorFusion(it)) },
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onClickEvent(
                    if (isRunning) ChannelSoundingEvent.StopRanging
                    else ChannelSoundingEvent.StartRanging
                )
            },
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (isRunning) R.string.action_stop else R.string.action_start
                )
            )
        }
    }
}

@Composable
private fun <T> DropdownOptionRow(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    enabled: Boolean,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Box {
            val contentColor =
                if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(contentColor.copy(alpha = 0.1f))
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = labelOf(selected),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = contentColor,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labelOf(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                        trailingIcon = {
                            if (option == selected) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveReadoutsCard(rangingData: CsRangingData) {
    ScreenSection {
        SectionTitle(
            icon = Icons.Default.Info,
            title = stringResource(R.string.ranging_measurement),
        )
        DistanceDashboard(rangingData.distance?.measurement)
        rangingData.distanceStdDevMeters?.let {
            LabeledRow(
                label = stringResource(R.string.standard_deviation),
                value = stringResource(R.string.ranging_distance_m, it.toFloat()),
            )
        }
        rangingData.delaySpreadMeters?.let {
            LabeledRow(
                label = stringResource(R.string.delay_spread),
                value = stringResource(R.string.ranging_distance_m, it.toFloat()),
            )
        }
        rangingData.velocityMetersPerSec?.let {
            LabeledRow(
                label = stringResource(R.string.velocity),
                value = stringResource(R.string.ranging_velocity_mps, it.toFloat()),
            )
        }
        rangingData.detectedAttackLevelPercent?.let {
            LabeledRow(
                label = stringResource(R.string.detected_attack_level),
                value = stringArrayResource(R.array.ranging_attack_level).getOrElse(it) { stringResource(R.string.ranging_attack_level_unknown) },
            )
        }
        rangingData.remoteTxPowerDbm?.let {
            LabeledRow(
                label = stringResource(R.string.remote_power_level),
                value = stringResource(R.string.ranging_rssi_dbm, it),
            )
        }
        rangingData.rssi?.let {
            LabeledRow(
                label = stringResource(R.string.rssi_label),
                value = stringResource(R.string.ranging_rssi_dbm, it),
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.signal_strength),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            SignalStrengthBar(rangingData.distance?.confidenceLevel?.value)
        }
    }
}

@Composable
private fun DistanceDashboard(measurement: Double?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // get measurement in string with 2 decimal places
        if (measurement == null || measurement < 0.01) {
            AnimatedThreeDots(
                modifier = Modifier.height(58.dp),
                dotSize = 16.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = stringResource(R.string.ranging_distance_m, measurement.toFloat()),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.height(58.dp),
            )
        }
    }
}

@Composable
private fun LabeledRow(
    label: String,
    extra: String? = null,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            extra?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
            color = valueColor,
        )
    }
}

@Composable
private fun technologyLabel(availability: TechnologyAvailability): String =
    availability.technology?.let { stringResource(it.toUiString()) }
        ?: stringResource(R.string.ranging_tech_unknown, availability.rawValue)

@Composable
private fun technologyExtra(availability: TechnologyAvailability): String? =
    availability.technology?.toUiExtraString()?.let { stringResource(it) }

@Composable
private fun InlineError(reason: SessionCloseReasonProvider) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(reason.toUiString()),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SessionError(reason: SessionCloseReasonProvider) {
    ScreenSection(horizontalAlignment = Alignment.CenterHorizontally) {
        SectionTitle(
            icon = Icons.Default.SocialDistance,
            title = stringResource(R.string.channel_sounding),
            tint = MaterialTheme.colorScheme.error,
        )
        Image(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
        )
        Text(
            text = stringResource(reason.toUiString()),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RecentMeasurementsChart(previousMeasurements: List<Float>) {
    OutlinedCard {
        SectionTitle(
            icon = Icons.AutoMirrored.Filled.ShowChart,
            title = stringResource(R.string.ranging_previous_measurement),
            modifier = Modifier.padding(16.dp)
        )
        RecentMeasurementChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp),
            previousData = previousMeasurements
        )
    }
}

/**
 * Fatal reasons take over the whole screen because the user cannot recover from them by simply
 * starting a session with different options.
 */
private fun SessionCloseReasonProvider.isFatal(): Boolean = this == SessionClosedReason.TOO_OLD ||
        this == SessionClosedReason.NOT_SUPPORTED ||
        this == SessionClosedReason.RANGING_NOT_AVAILABLE ||
        this == SessionClosedReason.CS_SECURITY_NOT_AVAILABLE

@Preview
@Composable
private fun ConfigCard_Preview() {
    ConfigCard(
        config = RangingOptions(),
        isRunning = false,
        onClickEvent = {},
    )
}

@Preview
@Composable
private fun ConfigCardRunning_Preview() {
    ConfigCard(
        config = RangingOptions(),
        isRunning = true,
        onClickEvent = {},
    )
}

@Preview
@Composable
private fun CapabilitiesCard_Preview() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    CapabilitiesCard(
        HostCapabilities(
            technologies = listOf(
                TechnologyAvailability(RangingTechnology.BLE_CS, 1, CapabilityStatus.ENABLED),
                TechnologyAvailability(RangingTechnology.UWB, 0, CapabilityStatus.NOT_SUPPORTED),
                TechnologyAvailability(RangingTechnology.WIFI_NAN_RTT, 0, CapabilityStatus.DISABLED_REGULATORY),
            ),
            channelSoundingSupported = true,
            supportedSecurityLevels = listOf(1),
        ),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    )
}

@Preview(showBackground = true)
@Composable
private fun LiveReadoutsCard_Preview() {
    LiveReadoutsCard(
        CsRangingData(
            technology = RangingTechnology.BLE_CS,
            timeStamp = 0L,
            rssi = -50,
            distance = CSRangingMeasurement(10.0, confidenceLevel = ConfidenceLevel.CONFIDENCE_MEDIUM),
            distanceStdDevMeters = 2.1,
            delaySpreadMeters = 0.1,
            velocityMetersPerSec = 1.1,
            detectedAttackLevelPercent = 4,
            remoteTxPowerDbm = -3, // dBm
        )
    )
}

@Preview
@Composable
private fun SessionError_Preview() {
    SessionError(reason = SessionClosedReason.NOT_SUPPORTED)
}
