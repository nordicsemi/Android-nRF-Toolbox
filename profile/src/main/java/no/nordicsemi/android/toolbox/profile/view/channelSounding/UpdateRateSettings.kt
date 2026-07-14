package no.nordicsemi.android.toolbox.profile.view.channelSounding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.UpdateRate

@Composable
internal fun UpdateRateSettings(
    selectedItem: UpdateRate,
    onItemSelected: (UpdateRate) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Box {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { isExpanded = !isExpanded }
        )
        // Show AlertDialog when isExpanded is true
        AnimatedVisibility(isExpanded) {
            // Your AlertDialog content here
            UpdateRateDialog(
                selectedUpdateRate = selectedItem,
                onConfirmation = onItemSelected,
                onDismiss = { isExpanded = false }
            )
        }
    }
}

@Composable
internal fun UpdateRateDialog(
    selectedUpdateRate: UpdateRate,
    onDismiss: () -> Unit,
    onConfirmation: (UpdateRate) -> Unit,
) {
    val updateOptions = UpdateRate.entries
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(selectedUpdateRate) }

    AlertDialog(
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.change_update_rate),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.select_new_update_rate),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        text = {
            Content(
                updateOptions = updateOptions,
                selectedOption = selectedOption,
                onItemSelected = onOptionSelected
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirmation(selectedOption)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
            ) {
                Text(stringResource(R.string.update_rate_confirm))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // looks "muted"
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
            ) {
                Text(stringResource(R.string.update_rate_cancel))
            }
        }
    )
}

@Composable
private fun Content(
    updateOptions: List<UpdateRate>,
    selectedOption: UpdateRate,
    onItemSelected: (UpdateRate) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedCard(
            modifier = Modifier.selectableGroup()
        ) {
            updateOptions.forEach { rate ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = rate == selectedOption,
                            onClick = { onItemSelected(rate) },
                            role = Role.RadioButton,
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(rate.toUiString()),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(rate.description()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = rate == selectedOption,
                        onClick = null // null recommended for accessibility with screen readers
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
            )
            Text(text = stringResource(R.string.update_rate_change_warning))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateRateDialogPreview() {
    var selectedOption by remember { mutableStateOf(UpdateRate.NORMAL) }
    UpdateRateSettings(
        selectedItem = selectedOption,
        onItemSelected = { selectedOption = it },
    )
}

@Preview(showBackground = true)
@Composable
private fun UpdateRateContentPreview() {
    var selectedOption by remember { mutableStateOf(UpdateRate.NORMAL) }
    Content(
        updateOptions = UpdateRate.entries,
        selectedOption = selectedOption,
        onItemSelected = { selectedOption = it },
    )
}
