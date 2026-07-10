package no.nordicsemi.android.ui.view.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.ui.R
import no.nordicsemi.android.ui.view.TextWithAnimatedDots

@Composable
fun ServiceDiscoveryView(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(PaddingValues) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedCard(
            modifier = Modifier
                .widthIn(max = 460.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothConnected,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                )

                TextWithAnimatedDots(
                    text = stringResource(id = R.string.discovering_services),
                    textStyle = MaterialTheme.typography.titleMedium
                )

                TextWithAnimatedDots(
                    text = stringResource(id = R.string.discovering_services_des),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }

        content(PaddingValues(top = 16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceDiscoveryViewPreview() {
    MaterialTheme {
        ServiceDiscoveryView(
            modifier = Modifier.padding(16.dp)
        ) { padding ->
            Button(
                onClick = {},
                modifier = Modifier.padding(padding)
            ) {
                Text(text = "Cancel")
            }
        }
    }
}
