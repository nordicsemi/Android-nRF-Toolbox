package no.nordicsemi.android.toolbox.profile.view.uart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.toolbox.profile.viewmodel.UARTEvent
import no.nordicsemi.android.toolbox.profile.viewmodel.UartViewModel

@Composable
internal fun UARTScreen(maxValueLength: Int?) {
    val uartViewModel = hiltViewModel<UartViewModel>()
    val state by uartViewModel.state.collectAsStateWithLifecycle()
    val onEvent: (UARTEvent) -> Unit = { uartViewModel.onEvent(it) }

    LaunchedEffect(key1 = maxValueLength != null) {
        if (maxValueLength != null)
            onEvent(UARTEvent.SetMaxValueLength(maxValueLength))
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MacroSection(state.uartViewState, onEvent)
        OutputSection(
            records = state.messages,
            onEvent = onEvent
        )
    }
}
