package no.nordicsemi.android.toolbox.profile.view.lbs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ModeStandby
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.manager.LBSManager
import no.nordicsemi.android.toolbox.profile.viewmodel.LBSEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.LBSViewModel
import no.nordicsemi.android.ui.view.ScreenSection

@Composable
internal fun BlinkyScreen(manager: LBSManager) {
    val lbsViewModel = hiltViewModel<LBSViewModel, LBSViewModel.Factory>(
        key = manager.instanceId,
        creationCallback = { factory -> factory.create(manager) }
    )
    val onClickEvent: (LBSEvent) -> Unit = { lbsViewModel.onEvent(it) }
    val serviceData by lbsViewModel.state.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LedControlView(
            ledState = serviceData.data.ledState,
            onStateChanged = { onClickEvent(LBSEvent.OnLedStateChanged(it)) },
        )
        ButtonControlView(
            buttonState = serviceData.data.buttonState,
        )
    }
}

@Composable
private fun LedControlView(
    ledState: Boolean,
    onStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenSection(
        modifier = modifier
            .clickable { onStateChanged(!ledState) }
    ) {
        SectionTitle(
            icon = if (ledState)
                Icons.Default.Lightbulb
            else Icons.Outlined.Lightbulb,
            title = stringResource(id = R.string.lbs_led),
            tint = if (ledState)
                colorResource(R.color.nordicSun)
            else MaterialTheme.colorScheme.primary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.lbs_led_guide),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = ledState,
                onCheckedChange = onStateChanged
            )
        }
    }
}

@Composable
private fun ButtonControlView(
    buttonState: Boolean,
    modifier: Modifier = Modifier,
) {
    ScreenSection(
        modifier = modifier,
    ) {
        SectionTitle(
            icon = if (buttonState)
                Icons.Default.ModeStandby
            else Icons.Default.RadioButtonChecked,
            title = stringResource(id = R.string.lbs_button),
        )
        Row {
            Text(
                text = stringResource(id = R.string.lbs_button_guide),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (buttonState)
                    stringResource(id = R.string.lbs_button_pressed)
                else stringResource(id = R.string.lbs_button_released),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LecControlViewPreview() {
    LedControlView(
        ledState = false,
        onStateChanged = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ButtonControlViewPreview() {
    ButtonControlView(
        buttonState = false,
    )
}
