package no.nordicsemi.android.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private class PlaceholderTransformation(private val placeholder: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return placeholderFilter(placeholder)
    }
}

fun placeholderFilter(placeholder: String): TransformedText {

    val numberOffsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return 0
        }

        override fun transformedToOriginal(offset: Int): Int {
            return 0
        }
    }

    return TransformedText(AnnotatedString(placeholder), numberOffsetTranslator)
}

/**
 * Compose view to input text in OutlinedTextField.
 */
@Composable
fun TextInputField(
    modifier: Modifier = Modifier,
    input: String,
    label: String,
    hint: String? = null,
    placeholder: String = "",
    errorMessage: String = "",
    errorState: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onUpdate: (String) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (input.isEmpty()) 0.5f else LocalContentColor.current.alpha
    )
    OutlinedTextField(
        value = input,
        onValueChange = onUpdate,
        visualTransformation = if (input.isEmpty())
            PlaceholderTransformation(placeholder) else VisualTransformation.None,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label) },
        keyboardOptions = keyboardOptions,
        placeholder = {
            Text(
                text = placeholder,
            )
        },
        supportingText = {
            if (errorState) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.alpha(1f)
                    )
                }
            } else if (hint != null) {
                Text(
                    text = hint,
                    modifier = Modifier.alpha(0.38f)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(textColor),
        isError = errorState,
    )
}