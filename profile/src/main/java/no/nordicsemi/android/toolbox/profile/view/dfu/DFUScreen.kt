package no.nordicsemi.android.toolbox.profile.view.dfu

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.DFUsAvailable
import no.nordicsemi.android.toolbox.profile.manager.DFUManager
import no.nordicsemi.android.toolbox.profile.viewmodel.ConnectionEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.DFUViewModel

@Composable
internal fun DFUScreen(manager: DFUManager, onRedirection: (ConnectionEvent.DisconnectEvent) -> Unit) {
    val dfuViewModel = hiltViewModel<DFUViewModel, DFUViewModel.Factory>(
        key = manager.instanceId,
        creationCallback = { factory -> factory.create(manager) }
    )
    val dfuServiceState by dfuViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    dfuServiceState.dfuAppName?.let { dfuApp ->
        val intent = context.packageManager.getLaunchIntentForPackage(dfuApp.packageName)
        val description =
            intent?.let {
                stringResource(R.string.dfu_description_open, stringResource(dfuApp.appName))
            } ?: stringResource(R.string.dfu_description_download)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DFUInstructionsCard(dfuApp)
            Spacer(modifier = Modifier.height(16.dp))
            DFUActionButton(dfuApp, intent, description, onRedirection)
        }
    }
}

@Composable
private fun DFUInstructionsCard(
    dfuApp: DFUsAvailable,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(dfuApp.appIcon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = stringResource(
                    R.string.dfu_not_supported_title,
                    stringResource(dfuApp.serviceName)
                ),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(
                    R.string.dfu_not_supported_text,
                    stringResource(dfuApp.serviceName),
                    stringResource(dfuApp.appName)
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DFUActionButton(
    dfuApp: DFUsAvailable,
    intent: Intent?,
    title: String,
    onRedirection: (ConnectionEvent.DisconnectEvent) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Button(
        onClick = {
            intent?.let { context.startActivity(it) }
                ?: uriHandler.openUri(dfuApp.appLink)
            onRedirection(ConnectionEvent.DisconnectEvent)
        }
    ) {
        val icon = intent?.let { dfuApp.appIcon } ?: R.drawable.google_play_2022_icon
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp),
            tint = if (intent == null) Color.Unspecified else MaterialTheme.colorScheme.onPrimary
        )

        Text(text = title)
    }
}

@Preview
@Composable
private fun DFUInstructionsCardPreview() {
    DFUInstructionsCard(DFUsAvailable.DFU_SERVICE)
}

@Preview
@Composable
private fun DFUActionButtonPreview() {
    DFUActionButton(
        dfuApp = DFUsAvailable.DFU_SERVICE,
        intent = null,
        title = "Download",
        onRedirection = {},
    )
}

private const val DFU_PACKAGE_NAME = "no.nordicsemi.android.dfu"
private const val DFU_APP_LINK =
    "https://play.google.com/store/apps/details?id=no.nordicsemi.android.dfu"

private const val SMP_PACKAGE_NAME = "no.nordicsemi.android.nrfconnectdevicemanager"
private const val SMP_APP_LINK =
    "https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrfconnectdevicemanager"

private val DFUsAvailable.packageName: String
    get() = when (this) {
        DFUsAvailable.DFU_SERVICE,
        DFUsAvailable.LEGACY_DFU_SERVICE,
        DFUsAvailable.EXPERIMENTAL_BUTTONLESS_DFU_SERVICE -> DFU_PACKAGE_NAME
        DFUsAvailable.SMP_SERVICE -> SMP_PACKAGE_NAME
    }

private val DFUsAvailable.appLink: String
    get() = when (this) {
        DFUsAvailable.DFU_SERVICE,
        DFUsAvailable.LEGACY_DFU_SERVICE,
        DFUsAvailable.EXPERIMENTAL_BUTTONLESS_DFU_SERVICE -> DFU_APP_LINK
        DFUsAvailable.SMP_SERVICE -> SMP_APP_LINK
    }

private val DFUsAvailable.appName: Int
    get() = when (this) {
        DFUsAvailable.DFU_SERVICE,
        DFUsAvailable.LEGACY_DFU_SERVICE,
        DFUsAvailable.EXPERIMENTAL_BUTTONLESS_DFU_SERVICE -> R.string.dfu_app_name
        DFUsAvailable.SMP_SERVICE -> R.string.smp_app_name
    }

private val DFUsAvailable.appIcon: Int
    get() = when (this) {
        DFUsAvailable.DFU_SERVICE,
        DFUsAvailable.LEGACY_DFU_SERVICE,
        DFUsAvailable.EXPERIMENTAL_BUTTONLESS_DFU_SERVICE -> R.drawable.ic_dfu
        DFUsAvailable.SMP_SERVICE -> R.drawable.ic_device_manager
    }

private val DFUsAvailable.serviceName: Int
    get() = when (this) {
        DFUsAvailable.LEGACY_DFU_SERVICE -> R.string.legacy_dfu_service_name
        DFUsAvailable.DFU_SERVICE,
        DFUsAvailable.EXPERIMENTAL_BUTTONLESS_DFU_SERVICE -> R.string.dfu_service_name
        DFUsAvailable.SMP_SERVICE -> R.string.smp_service_name
    }
