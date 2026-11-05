package no.nordicsemi.android.toolbox.profile.view.uart

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.UARTViewState
import no.nordicsemi.android.toolbox.profile.data.uart.UARTConfiguration
import no.nordicsemi.android.toolbox.profile.viewmodel.UARTEvent

@Composable
internal fun MacroSection(
    viewState: UARTViewState = UARTViewState(),
    onEvent: (UARTEvent) -> Unit
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // Dialogs
    MacroDialogs(
        viewState = viewState,
        showAddDialog = showAddDialog,
        showDeleteDialog = showDeleteDialog,
        onDismissAdd = { showAddDialog = false },
        onDismissDelete = { showDeleteDialog = false },
        onEvent = onEvent
    )

    OutlinedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MacroSectionTitle(
                onAddClick = { showAddDialog = true }
            )

            if (viewState.configurations.isNotEmpty()) {
                MacroConfigControls(
                    viewState = viewState,
                    onDeleteClick = { showDeleteDialog = true },
                    onEvent = onEvent
                )

                viewState.selectedConfiguration?.let {
                    UARTMacroView(it, viewState.isConfigurationEdited, onEvent)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MacroSectionPreview() {
    MacroSection(
        viewState = UARTViewState(
            configurations = listOf(
                UARTConfiguration(1, "Config 1"),
                UARTConfiguration(2, "Config 2")
            ),
            selectedConfigurationName = "Config 1",
            isConfigurationEdited = false
        )
    ) {}
}

@Composable
private fun MacroDialogs(
    viewState: UARTViewState,
    showAddDialog: Boolean,
    showDeleteDialog: Boolean,
    onDismissAdd: () -> Unit,
    onDismissDelete: () -> Unit,
    onEvent: (UARTEvent) -> Unit
) {
    if (showAddDialog) {
        UARTAddConfigurationDialog(viewState, onEvent, onDismissAdd)
    }

    if (showDeleteDialog) {
        viewState.selectedConfiguration?.let {
            DeleteConfigurationDialog(it, onEvent, onDismissDelete)
        }
    }

    if (viewState.showEditDialog) {
        UARTAddMacroDialog(viewState.selectedMacro) { onEvent(it) }
    }
}

@Composable
private fun MacroSectionTitle(
    onAddClick: () -> Unit,
) {
    SectionTitle(
        painter = painterResource(R.drawable.ic_macro),
        title = stringResource(id = R.string.uart_macros),
        menu = {
            CircleIcon(
                imageVector = Icons.Default.Add, R.string.uart_configuration_add,
                onClick = onAddClick,
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun MacroSectionTitlePreview() {
    MacroSectionTitle(onAddClick = {})
}

@Composable
private fun MacroConfigControls(
    viewState: UARTViewState,
    onDeleteClick: () -> Unit,
    onEvent: (UARTEvent) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            UARTConfigurationPicker(viewState, onEvent)
        }

        viewState.selectedConfiguration?.let {
            val editIcon =
                if (viewState.isConfigurationEdited) Icons.Default.EditOff else Icons.Default.Edit
            val editDesc = R.string.uart_configuration_edit

            CircleIcon(editIcon, editDesc) {
                onEvent(UARTEvent.OnEditConfiguration)
            }

            CircleIcon(
                Icons.Default.Delete,
                R.string.uart_configuration_delete,
                MaterialTheme.colorScheme.error
            ) { onDeleteClick() }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MacroConfigControlsPreview() {
    MacroConfigControls(
        viewState = UARTViewState(
            configurations = listOf(
                UARTConfiguration(1, "Config 1"),
                UARTConfiguration(2, "Config 2")
            ),
            selectedConfigurationName = "Config 1",
            isConfigurationEdited = false
        ),
        onDeleteClick = {},
        onEvent = {}
    )
}

@Composable
private fun CircleIcon(
    imageVector: ImageVector,
    @StringRes contentDescription: Int,
    tintColor: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    Icon(
        imageVector = imageVector,
        contentDescription = stringResource(id = contentDescription),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        tint = tintColor,
    )
}

@Composable
private fun DeleteConfigurationDialog(
    selectedConfiguration: UARTConfiguration,
    onEvent: (UARTEvent) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(id = R.string.uart_delete_dialog_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Text(text = stringResource(id = R.string.uart_delete_dialog_info))
        },
        confirmButton = {
            TextButton(onClick = {
                onEvent(UARTEvent.OnDeleteConfiguration(selectedConfiguration))
                onDismiss()
            }) {
                Text(text = stringResource(id = R.string.uart_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.uart_delete_dialog_cancel))
            }
        }
    )
}

@Preview
@Composable
private fun DeleteConfigurationDialogPreview() {
    DeleteConfigurationDialog(UARTConfiguration(null, "Config 1"), {}, {})
}