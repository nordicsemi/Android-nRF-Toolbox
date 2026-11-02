package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.manager.LBSManager

internal sealed interface LBSEvent {
    data class OnLedStateChanged(val value: Boolean) : LBSEvent
    data class OnButtonStateChanged(val value: Boolean) : LBSEvent
    /** Blinks the LED [LBSViewModel.BLINK_COUNT] times. */
    data object OnBlink : LBSEvent
    data class OnBindingChanged(val value: Boolean) : LBSEvent
}

@HiltViewModel(assistedFactory = LBSViewModel.Factory::class)
internal class LBSViewModel @AssistedInject constructor(
    @Assisted private val manager: LBSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: LBSManager): LBSViewModel }

    companion object {
        const val BLINK_COUNT = 3
    }

    val state = manager.repository.data

    /** The state of the Button -> LED binding. */
    val bindingState = manager.bindingState

    /** The flow of button click events. */
    val buttonPressed = manager.buttonPressed

    /** The flow of button long click events. */
    val buttonLongPressed = manager.buttonLongPressed

    fun onEvent(event: LBSEvent) {
        when (event) {
            is LBSEvent.OnLedStateChanged -> viewModelScope.launch {
                manager.writeLED(event.value)
            }

            LBSEvent.OnBlink -> viewModelScope.launch {
                manager.blink(BLINK_COUNT)
            }

            is LBSEvent.OnBindingChanged -> manager.bindingState.value = event.value

            is LBSEvent.OnButtonStateChanged ->
                manager.repository.updateButtonState(event.value)
        }
    }
}
