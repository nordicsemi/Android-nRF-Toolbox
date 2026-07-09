package no.nordicsemi.android.nrftoolbox.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
    ) {
        ListItem(
            headlineContent = { Text(deviceName ?: stringResource(R.string.unknown_device)) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(deviceAddress)
                    FlowRow(
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
            },
            leadingContent = {
                Icon(
                    painter = icon,
                    contentDescription = description,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        )
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
