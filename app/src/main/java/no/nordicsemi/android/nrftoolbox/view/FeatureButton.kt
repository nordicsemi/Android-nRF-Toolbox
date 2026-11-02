package no.nordicsemi.android.nrftoolbox.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.theme.NordicTheme
import no.nordicsemi.android.nrftoolbox.R
import kotlin.math.absoluteValue

@Composable
internal fun FeatureButton(
    icon: Painter,
    description: String,
    profileNames: List<String> = listOf(description),
    deviceName: String?,
    deviceAddress: String,
    isConnected: Boolean = true,
    values: List<DeviceValue> = emptyList(),
    onDisconnect: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
    ) {
        // The content of an offline device (waiting for reconnection) is dimmed.
        val dimmed = Modifier.alpha(if (isConnected) 1f else 0.5f)
        // The layout follows ListItem: 16 dp horizontal padding and 16 dp between the icon and
        // the text. The icon is centered between the name and the address, not including the
        // profile badges, and the trailing content is centered in the whole card.
        Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                    .then(dimmed),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = description,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = deviceName ?: stringResource(R.string.unknown_device),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = deviceAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier.padding(start = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    maxLines = 1,
                ) {
                    profileNames.forEach {
                        Badge(
                            containerColor = vibrantColorFromString(it),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(1.dp),
                            )
                        }
                    }
                }
            }
            onDisconnect?.let {
                // The icon of the 48 dp button is inset by 12 dp, so with 4 dp it ends 16 dp
                // from the edge, like the trailing icons of ListItem on the Links cards.
                Box(modifier = Modifier.padding(end = 4.dp)) {
                    Disconnect(isReconnecting = !isConnected, onClick = it)
                }
            }
        }
        // The footer: the most important values of the services, separated from the device
        // info with a line.
        if (values.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FlowRow(
                // The 16 dp icon is centered below the 24 dp icon of the device.
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                values.forEach { DeviceValueItem(it) }
            }
        }
        }
    }
}

@Composable
private fun DeviceValueItem(value: DeviceValue) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = value.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = value.text,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * Disconnects the device. When the device is reconnecting, a progress is shown around the button.
 */
@Composable
private fun Disconnect(isReconnecting: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(
                    if (isReconnecting) R.string.stop_reconnecting else R.string.disconnect
                ),
                // Same style as the "open in another app" icons on the Links cards.
                modifier = Modifier.size(24.dp).alpha(0.6f),
            )
        }
        if (isReconnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Preview(heightDp = 100)
@Composable
private fun FeatureButtonPreview() {
    NordicTheme {
        FeatureButton(
            icon = painterResource(R.drawable.ic_quick_start),
            description = stringResource(R.string.quick_start_module_full),
            profileNames = listOf("Quick Start", "LED Button", "MDS"),
            deviceName = "Quick Start",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            values = listOf(DeviceValue(Icons.Default.BatteryFull, "82%"), DeviceValue(Icons.Default.Favorite, "72 bpm")),
            onDisconnect = {},
            onClick = {}
        )
    }
}

@Preview(heightDp = 120)
@Composable
private fun FeatureButtonPreview_Offline() {
    NordicTheme {
        FeatureButton(
            icon = painterResource(R.drawable.ic_quick_start),
            description = stringResource(R.string.quick_start_module_full),
            profileNames = listOf("Quick Start", "LED Button", "MDS"),
            deviceName = "Quick Start",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            isConnected = false,
            onDisconnect = {},
            onClick = {}
        )
    }
}

@Preview(heightDp = 100)
@Composable
private fun FeatureButtonPreview_2() {
    NordicTheme {
        FeatureButton(
            icon = painterResource(R.drawable.ic_csc),
            description = stringResource(R.string.csc_module_full),
            profileNames = listOf("Battery", "Heart Rate", "Blood Pressure"),
            deviceName = "Testing peripheral",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            onClick = {}
        )
    }
}

private fun vibrantColorFromString(input: String): Color {
    val hash = input.hashCode() and 0x7FFFFFFF
    // Using the Golden Ratio conjugate (≈ 0.618) to spread hues uniformly in HSL space.
    // This ensures that even strings with similar hashes will result in distinct colors.
    val goldenRatioConjugate = 0.618033988749895
    val hue = ((1.286 * hash.toDouble() * goldenRatioConjugate) % 1.0).toFloat() * 360f

    // Return a vibrant color with fixed saturation and lightness for consistency and readability.
    return Color.hsl(hue = hue, saturation = 0.75f, lightness = 0.45f)
}
