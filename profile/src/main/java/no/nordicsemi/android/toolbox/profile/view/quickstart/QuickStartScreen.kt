package no.nordicsemi.android.toolbox.profile.view.quickstart

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.ui.view.ScreenSection

@Composable
internal fun QuickStartScreen() {
    ScreenSection {
        SectionTitle(
            painter = painterResource(R.drawable.ic_quick_start),
            title = stringResource(R.string.quick_start_title),
        )
        Image(
            painter = painterResource(R.drawable.dk),
            contentDescription = stringResource(R.string.quick_start_title),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(220.dp)
                .aspectRatio(800f / 434f)
                .clip(MaterialTheme.shapes.medium),
        )
        Text(
            text = stringResource(R.string.quick_start_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
//        HorizontalDivider()
//        QuickStartTip(
//            icon = Icons.Default.Lightbulb,
//            text = stringResource(R.string.quick_start_lbs_led_tip),
//        )
//        QuickStartTip(
//            icon = Icons.Default.RadioButtonChecked,
//            text = stringResource(R.string.quick_start_lbs_button_tip),
//        )
//        QuickStartTip(
//            icon = Icons.Default.BugReport,
//            text = stringResource(R.string.quick_start_crash_tip),
//            tint = MaterialTheme.colorScheme.error,
//        )
    }
}

@Composable
private fun QuickStartTip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickStartScreenPreview() {
    QuickStartScreen()
}
