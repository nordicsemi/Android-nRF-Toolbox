package no.nordicsemi.android.toolbox.profile.view.channelSounding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.theme.nordicFall
import no.nordicsemi.android.common.theme.nordicGreen
import no.nordicsemi.android.common.theme.nordicRed
import no.nordicsemi.android.toolbox.profile.data.ConfidenceLevel

@Preview(showBackground = true)
@Composable
internal fun SignalStrengthBar(confidenceLevel: Int? = ConfidenceLevel.CONFIDENCE_HIGH.value) {
    val (signalColor, strengthFraction) = when (confidenceLevel) {
        ConfidenceLevel.CONFIDENCE_HIGH.value -> Pair(nordicGreen, 1.0f)
        ConfidenceLevel.CONFIDENCE_MEDIUM.value -> Pair(nordicFall, 0.66f)
        ConfidenceLevel.CONFIDENCE_LOW.value -> Pair(nordicRed, 0.33f)
        else -> Pair(MaterialTheme.colorScheme.primary, 0.0f)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "signal-loading")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetX"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            signalColor.copy(alpha = 0.75f),
            signalColor,
            signalColor.copy(alpha = 0.75f)
        ),
        start = Offset(offsetX * 200f, 0f),
        end = Offset((offsetX + 1f) * 200f, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(strengthFraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(brush)
        )
    }
}
