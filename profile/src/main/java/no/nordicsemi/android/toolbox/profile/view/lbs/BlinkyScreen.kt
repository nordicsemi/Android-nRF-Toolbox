package no.nordicsemi.android.toolbox.profile.view.lbs

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.ModeStandby
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.manager.LBSManager
import no.nordicsemi.android.toolbox.profile.viewmodel.LBSEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.LBSViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun BlinkyScreen(manager: LBSManager) {
    val lbsViewModel = hiltViewModel<LBSViewModel, LBSViewModel.Factory>(
        key = manager.instanceId,
        creationCallback = { factory -> factory.create(manager) }
    )
    val onClickEvent: (LBSEvent) -> Unit = { lbsViewModel.onEvent(it) }
    val serviceData by lbsViewModel.state.collectAsStateWithLifecycle()
    val bindingState by lbsViewModel.bindingState.collectAsStateWithLifecycle()

    BlinkyControlView(
        ledState = serviceData.data.ledState,
        onStateChanged = { onClickEvent(LBSEvent.OnLedStateChanged(it)) },
        onBlink = { onClickEvent(LBSEvent.OnBlink) },
        bindingState = bindingState,
        onBindingChanged = { onClickEvent(LBSEvent.OnBindingChanged(it)) },
        buttonState = serviceData.data.buttonState,
        buttonPressed = lbsViewModel.buttonPressed,
        buttonLongPressed = lbsViewModel.buttonLongPressed,
    )
}

@Composable
private fun BlinkyControlView(
    ledState: Boolean,
    onStateChanged: (Boolean) -> Unit,
    onBlink: () -> Unit,
    bindingState: Boolean,
    onBindingChanged: (Boolean) -> Unit,
    buttonState: Boolean,
    buttonPressed: Flow<Unit>,
    buttonLongPressed: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LedControlView(
            state = ledState,
            enabled = !bindingState,
            onStateChanged = onStateChanged,
            onBlink = onBlink,
        )

        Binding(
            on = bindingState,
            onBindingChanged = onBindingChanged,
        )

        ButtonControlView(
            state = buttonState,
            buttonPressed = buttonPressed,
            buttonLongPressed = buttonLongPressed,
        )
    }
}

/** A link between the Button and the LED. When on, the LED follows the button. */
@Composable
private fun Binding(
    on: Boolean,
    onBindingChanged: (Boolean) -> Unit,
    connectionHeight: Dp = 16.dp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerticalDivider(
            modifier = Modifier.height(connectionHeight)
        )

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
            OutlinedIconToggleButton(
                checked = on,
                onCheckedChange = onBindingChanged,
                border = if (on) null else BorderStroke(1.dp, DividerDefaults.color),
                modifier = Modifier.defaultMinSize(minHeight = 1.dp),
            ) {
                Icon(
                    imageVector = if (on) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = null,
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.height(connectionHeight)
        )
    }
}

@Composable
private fun LedControlView(
    state: Boolean,
    enabled: Boolean,
    onStateChanged: (Boolean) -> Unit,
    onBlink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = enabled) { onStateChanged(!state) }
                .padding(16.dp)
        ) {
            SectionTitle(
                icon = if (state) Icons.Default.Lightbulb else Icons.Outlined.Lightbulb,
                title = stringResource(R.string.lbs_led),
                tint = if (state) colorResource(R.color.nordicSun) else MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.lbs_led_guide),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state,
                    onCheckedChange = onStateChanged,
                    enabled = enabled,
                )
            }

            // Blink action.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.lbs_led_blink_guide, LBSViewModel.BLINK_COUNT),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onBlink,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LightMode,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.lbs_led_blink).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonControlView(
    state: Boolean,
    buttonPressed: Flow<Unit>,
    buttonLongPressed: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    var click by remember { mutableStateOf(false) }
    var longClick by remember { mutableStateOf(false) }

    LaunchedEffect(buttonPressed) {
        buttonPressed.collect {
            click = true
            delay(250.milliseconds)
            click = false
        }
    }
    LaunchedEffect(buttonLongPressed) {
        buttonLongPressed.collect {
            longClick = true
            delay(500.milliseconds)
            longClick = false
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (state) 1f else 0f,
        animationSpec = if (state)
            tween(LBSManager.LONG_PRESS_TIMEOUT.inWholeMilliseconds.toInt(), easing = LinearEasing)
        else spring()
    )

    OutlinedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            SectionTitle(
                icon = if (state) Icons.Default.ModeStandby else Icons.Default.RadioButtonChecked,
                title = stringResource(R.string.lbs_button),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.lbs_button_guide),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (state) {
                        stringResource(R.string.lbs_button_pressed)
                    } else {
                        stringResource(R.string.lbs_button_released)
                    },
                )
            }

            // Click indicator.
            EventIndicator(
                description = stringResource(R.string.lbs_button_click_guide),
                icon = Icons.Outlined.TouchApp,
                label = stringResource(R.string.lbs_button_click),
                active = click,
            )

            // Long click indicator.
            EventIndicator(
                description = stringResource(R.string.lbs_button_long_click_guide),
                icon = Icons.Outlined.WatchLater,
                label = stringResource(R.string.lbs_button_long_click),
                active = longClick,
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progress },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Legend.
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WatchLater,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(12.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.lbs_button_long_click_legend,
                            LBSManager.LONG_PRESS_TIMEOUT.inWholeSeconds
                        ).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** A row with a description and a pill, which is highlighted when the event is [active]. */
@Composable
private fun EventIndicator(
    description: String,
    icon: ImageVector,
    label: String,
    active: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = description,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .background(
                    color = if (active)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp)
                )
                .widthIn(min = 56.dp)
                .heightIn(min = 40.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlinkyControlViewPreview() {
    var bindingState by rememberSaveable { mutableStateOf(false) }

    BlinkyControlView(
        ledState = true,
        onStateChanged = {},
        onBlink = {},
        bindingState = bindingState,
        onBindingChanged = { bindingState = it },
        buttonState = true,
        buttonPressed = emptyFlow(),
        buttonLongPressed = emptyFlow(),
        modifier = Modifier.padding(16.dp),
    )
}
