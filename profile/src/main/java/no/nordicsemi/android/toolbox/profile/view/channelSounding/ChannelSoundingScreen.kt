package no.nordicsemi.android.toolbox.profile.view.channelSounding

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.theme.NordicTheme
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.ChannelSoundingServiceData
import no.nordicsemi.android.toolbox.profile.data.ConfidenceLevel
import no.nordicsemi.android.toolbox.profile.data.CsRangingData
import no.nordicsemi.android.toolbox.profile.data.RangingSessionAction
import no.nordicsemi.android.toolbox.profile.data.RangingSessionFailedReason
import no.nordicsemi.android.toolbox.profile.data.RangingTechnology
import no.nordicsemi.android.toolbox.profile.data.SessionCloseReasonProvider
import no.nordicsemi.android.toolbox.profile.data.SessionClosedReason
import no.nordicsemi.android.toolbox.profile.data.UpdateRate
import no.nordicsemi.android.toolbox.profile.manager.ChannelSoundingManager
import no.nordicsemi.android.toolbox.profile.viewmodel.ChannelSoundingEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.ChannelSoundingViewModel
import no.nordicsemi.android.ui.view.AnimatedThreeDots
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.TextWithAnimatedDots
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
        SessionError(
            reason = SessionClosedReason.NOT_SUPPORTED,
            isRestartingSession = false,
            onClickEvent = {},
        )
    }
}

@Composable
private fun ChannelSoundingView(
    channelSoundingState: ChannelSoundingServiceData,
    onClickEvent: (ChannelSoundingEvent) -> Unit,
) {
    var isRestartingSession by rememberSaveable { mutableStateOf(false) }
    when (val sessionData = channelSoundingState.rangingSessionAction) {
        is RangingSessionAction.OnError -> {
            SessionError(sessionData.reason, isRestartingSession) { onClickEvent(it) }
        }

        is RangingSessionAction.OnResult -> {
            RangingContent(
                channelSoundingState.updateRate,
                sessionData.data,
                sessionData.previousData,
            ) {
                isRestartingSession = true
                onClickEvent(it)
            }
        }

        RangingSessionAction.OnClosed -> {
            SessionClosed(isRestartingSession, onClickEvent)
        }

        RangingSessionAction.OnStart -> {
            isRestartingSession = false
            InitiatingSession()
        }

        RangingSessionAction.OnRestarting -> {
            isRestartingSession = true
            RestartingSession()
        }

        null -> LoadingView()
    }
}

@Composable
private fun InitiatingSession() {
    ScreenSection {
        SectionTitle(
            icon = Icons.Default.SocialDistance,
            title = stringResource(R.string.channel_sounding),
        )
        TextWithAnimatedDots(
            text = stringResource(R.string.initiating_ranging),
        )
    }
}

@Composable
private fun RestartingSession() {
    ScreenSection {
        SectionTitle(
            icon = Icons.Default.SocialDistance,
            title = stringResource(R.string.channel_sounding),
        )
        TextWithAnimatedDots(
            text = stringResource(R.string.ranging_session_restarting),
        )
    }
}

@Composable
private fun SessionClosed(
    isRestartingSession: Boolean,
    onClickEvent: (ChannelSoundingEvent) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenSection {
            SectionTitle(
                icon = Icons.Default.SocialDistance,
                title = stringResource(R.string.channel_sounding),
            )
            Text(
                stringResource(R.string.ranging_session_stopped),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (isRestartingSession) {
            Button(onClick = { /* No action */ }) {
                Text(text = stringResource(id = R.string.ranging_session_reconnecting))
            }
        } else {
            Button(
                onClick = { onClickEvent(ChannelSoundingEvent.RestartRangingSession) },
            ) {
                Text(text = stringResource(id = R.string.action_reconnect))
            }
        }
    }
}

@Composable
private fun SessionError(
    reason: SessionCloseReasonProvider,
    isRestartingSession: Boolean,
    onClickEvent: (ChannelSoundingEvent) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenSection(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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

        if (isRestartingSession && reason == RangingSessionFailedReason.LOCAL_REQUEST) {
            Button(onClick = { /* No action */ }) {
                Text(text = stringResource(id = R.string.ranging_session_reconnecting))
            }
        } else if (
            reason != SessionClosedReason.NOT_SUPPORTED &&
            reason != SessionClosedReason.RANGING_NOT_AVAILABLE
        ) {
            Button(
                onClick = { onClickEvent(ChannelSoundingEvent.RestartRangingSession) },
            ) {
                Text(text = stringResource(id = R.string.action_reconnect))
            }
        }
    }
}

@Composable
private fun RangingContent(
    updateRate: UpdateRate,
    rangingData: CsRangingData,
    previousMeasurements: List<Float> = emptyList(),
    onClickEvent: (ChannelSoundingEvent) -> Unit,
) {
    val distanceMeasurement = rangingData.distance?.measurement
    val confidence = rangingData.distance?.confidenceLevel?.value

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        distanceMeasurement?.let { measurement ->
            DistanceDashboard(measurement)
        }

        DetailsCard(
            updateRate = updateRate,
            rangingTechnology = rangingData.technology.value,
            confidenceLevel = confidence
        ) { onClickEvent(ChannelSoundingEvent.RangingUpdateRate(it)) }

        Spacer(modifier = Modifier.height(16.dp))
        RecentMeasurementsChart(previousMeasurements)

    }
}

@Composable
private fun DistanceDashboard(measurement: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // get measurement in string with 2 decimal places
        if (measurement < 0.01) {
            AnimatedThreeDots(dotSize = 16.dp)
        } else {
            Text(
                text = stringResource(R.string.ranging_distance_m, measurement.toFloat()),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displayLarge
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.current_measurement),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DetailsCard(
    updateRate: UpdateRate = UpdateRate.NORMAL,
    rangingTechnology: Int = RangingTechnology.BLE_CS.value,
    confidenceLevel: Int? = ConfidenceLevel.CONFIDENCE_HIGH.value,
    onUpdateRateSelected: (UpdateRate) -> Unit = { }
) {
    // Details Section
    Text(
        text = stringResource(R.string.ranging_details),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 16.dp)
            .alpha(0.5f)
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.ranging_technology),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(RangingTechnology.from(rangingTechnology)?.let {
                    stringResource(it.toUiString())
                } ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.update_rate),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                UpdateRateSettings(updateRate) { onUpdateRateSelected(it) }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(updateRate.toUiString()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.signal_strength),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                SignalStrengthBar(confidenceLevel)
            }
        }
    }
}

@Composable
private fun RecentMeasurementsChart(
    previousMeasurements: List<Float>,
) {
    // Recent Measurements
    Text(
        text = stringResource(R.string.ranging_previous_measurement),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 16.dp)
            .alpha(0.5f)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(8.dp)
    ) {
        RecentMeasurementChart(
            previousData = previousMeasurements
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DistanceDashboard_Preview() {
    DistanceDashboard(10.0)
}

@Preview(showBackground = true)
@Composable
private fun DistanceDashboardLoading_Preview() {
    DistanceDashboard(0.0)
}

@Preview
@Composable
private fun Restarting_Preview() {
    RestartingSession()
}

@Preview
@Composable
private fun DetailsCard_Preview() {
    DetailsCard()
}

@Preview
@Composable
private fun SessionClosed_Preview() {
    SessionClosed(
        isRestartingSession = false,
        onClickEvent = {}
    )
}

@Preview
@Composable
private fun SessionError_Preview() {
    SessionError(
        reason = SessionClosedReason.NOT_SUPPORTED,
        isRestartingSession = false,
        onClickEvent = {}
    )
}