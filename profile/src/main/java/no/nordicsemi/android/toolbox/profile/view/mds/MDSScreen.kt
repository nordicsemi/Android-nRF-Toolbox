package no.nordicsemi.android.toolbox.profile.view.mds

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.observability.ObservabilityManager
import no.nordicsemi.android.observability.data.Chunk
import no.nordicsemi.android.observability.data.ChunksSource
import no.nordicsemi.android.observability.data.ChunksConfig
import no.nordicsemi.android.observability.internet.ChunkManager
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.manager.MDSManager
import no.nordicsemi.android.toolbox.profile.viewmodel.MDSViewModel
import no.nordicsemi.android.ui.view.KeyValueColumn
import no.nordicsemi.android.ui.view.ScreenSection
import no.nordicsemi.android.ui.view.SectionRow

@Composable
internal fun MDSScreen(manager: MDSManager) {
    val mdsViewModel = hiltViewModel<MDSViewModel, MDSViewModel.Factory>(
        key = manager.instanceId,
        creationCallback = { factory -> factory.create(manager) }
    )
    val mdsState by mdsViewModel.state.collectAsStateWithLifecycle()

    ObservabilityView(mdsState)
}

@Composable
private fun ObservabilityView(
    state: ObservabilityManager.State,
    modifier: Modifier = Modifier,
) {
    ScreenSection(
        modifier = modifier,
    ) {
        SectionTitle(
            icon = Icons.Default.DeveloperBoard,
            title = stringResource(id = R.string.mds_title),
        )
        SectionRow {
            val status = when (state.state) {
                is ChunksSource.State.Disconnected -> stringResource(R.string.mds_disconnected)
                is ChunksSource.State.Initializing -> stringResource(R.string.mds_connecting)
                is ChunksSource.State.Connected -> {
                    when (val u = state.uploadingState) {
                        is ChunkManager.State.Idle -> stringResource(R.string.mds_connected)
                        is ChunkManager.State.InProgress -> stringResource(R.string.mds_uploading)
                        is ChunkManager.State.Suspended -> stringResource(R.string.mds_suspended, u.delayInSeconds)
                    }
                }
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        SectionRow {
            KeyValueColumn(
                key = stringResource(R.string.mds_pending),
                value = pluralStringResource(R.plurals.mds_value,  state.chunksPending, state.chunksPending, state.bytesPending)
            )
            KeyValueColumn(
                key = stringResource(R.string.mds_sent),
                value = pluralStringResource(R.plurals.mds_value, state.chunksUploaded,  state.chunksUploaded, state.bytesUploaded)
            )
        }
    }
}

@Preview
@Composable
private fun ObservabilityViewPreview() {
    ObservabilityView(
        state = ObservabilityManager.State(
            state = ChunksSource.State.Connected(
                config = ChunksConfig(
                    authorisationToken = "token",
                    url = "url",
                    deviceId = "deviceId",
                ),
            ),
            uploadingState = ChunkManager.State.InProgress,
            chunks = listOf(
                Chunk(0, byteArrayOf(1, 2, 3), "A", false),
                Chunk(1, byteArrayOf(1, 2, 3), "A", false),
                Chunk(2, byteArrayOf(1, 2, 3), "A", false),
                Chunk(3, byteArrayOf(1, 2, 3), "A", true),
                Chunk(4, byteArrayOf(1, 2, 3), "A", true),
            ),
        )
    )
}