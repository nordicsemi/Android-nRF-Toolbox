package no.nordicsemi.android.ui.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedThreeDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp
) {
    val dotCount = 3
    val infiniteTransition = rememberInfiniteTransition()

    val dotAlphas = List(dotCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500,
                    delayMillis = index * 200,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        dotAlphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .padding(1.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value))
            )
        }
    }
}

@Composable
fun TextWithAnimatedDots(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign = TextAlign.Center
) {
    Row(
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = text,
            textAlign = textAlign,
            style = textStyle,
        )
        Spacer(modifier = Modifier.width(2.dp))
        AnimatedThreeDots(
            modifier = modifier.padding(bottom = 4.dp),
            dotSize = dotSize
        )
    }
}
