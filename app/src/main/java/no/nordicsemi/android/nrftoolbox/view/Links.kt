package no.nordicsemi.android.nrftoolbox.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrftoolbox.R
import no.nordicsemi.android.nrftoolbox.viewmodel.UiEvent

@Composable
internal fun Links(onEvent: (UiEvent) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        OutlinedCard(
            shape = MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(4.dp), bottomEnd = CornerSize(4.dp)),
        ) {
            Link(
                icon = Icons.Outlined.Keyboard,
                title = stringResource(R.string.github_repo),
                subtitle = stringResource(R.string.github_repo_subtitle),
                onClick = { onEvent(UiEvent.OnGitHubClick) },
            )
        }

        OutlinedCard(
            shape = MaterialTheme.shapes.medium.copy(topStart = CornerSize(4.dp), topEnd = CornerSize(4.dp)),
        ) {
            Link(
                icon = Icons.Default.Support,
                title = stringResource(R.string.nordic_dev_zone),
                subtitle = stringResource(R.string.nordic_dev_zone_subtitle),
                onClick = { onEvent(UiEvent.OnNordicDevZoneClick) },
            )
        }
    }
}

@Composable
private fun Link(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { if (!subtitle.isNullOrEmpty()) Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.alpha(0.6f),
            )
        }
    )
}

@Preview
@Composable
private fun LinksPreview() {
    Links { }
}