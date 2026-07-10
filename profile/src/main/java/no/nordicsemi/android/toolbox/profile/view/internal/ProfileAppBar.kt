package no.nordicsemi.android.toolbox.profile.view.internal

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import no.nordicsemi.android.common.logger.view.LoggerAppBarIcon
import no.nordicsemi.android.common.theme.NordicTheme
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.toolbox.profile.viewmodel.ProfileUiState
import no.nordicsemi.android.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileAppBar(
    deviceName: String?,
    deviceAddress: String,
    connectionState: ProfileUiState,
    navigateUp: () -> Unit,
    disconnect: () -> Unit,
    openLogger: () -> Unit,
) {
    NordicAppBar(
        title = {
            Text(
                text = deviceName ?: deviceAddress,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        onNavigationButtonClick = navigateUp,
        actions = {
            TextButton(
                enabled = connectionState !is ProfileUiState.Disconnected,
                onClick = disconnect,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                )
            ) {
                Text(stringResource(id = R.string.disconnect))
            }
            LoggerAppBarIcon(onClick = openLogger)
        }
    )
}

@Preview
@Composable
private fun ProfileAppBarPreview() {
    NordicTheme {
        ProfileAppBar(
            deviceName = "Nordic HRM",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            connectionState = ProfileUiState.Loading,
            navigateUp = {},
            disconnect = {},
            openLogger = {},
        )
    }
}
