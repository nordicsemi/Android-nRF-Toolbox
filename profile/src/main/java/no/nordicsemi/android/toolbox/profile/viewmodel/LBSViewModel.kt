package no.nordicsemi.android.toolbox.profile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.manager.LBSManager
import timber.log.Timber

internal sealed interface LBSEvent {
    data class OnLedStateChanged(val value: Boolean) : LBSEvent
    data class OnButtonStateChanged(val value: Boolean) : LBSEvent
    /** Blinks the LED [LBSViewModel.BLINK_COUNT] times. */
    data object OnBlink : LBSEvent
    data class OnBindingChanged(val value: Boolean) : LBSEvent
}

@HiltViewModel(assistedFactory = LBSViewModel.Factory::class)
internal class LBSViewModel @AssistedInject constructor(
    @ApplicationContext private val context: Context,
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

    init {
        // Vibrate when the button is pressed or released, and when it is long pressed.
        manager.repository.data
            .map { it.data.buttonState }
            .distinctUntilChanged()
            // Skip the initial state.
            .drop(1)
            .onEach { vibrate(longClick = false) }
            .launchIn(viewModelScope)
        manager.buttonLongPressed
            .onEach { vibrate(longClick = true) }
            .launchIn(viewModelScope)
    }

    private fun vibrate(longClick: Boolean) {
        try {
            context.vibrate(longClick)
        } catch (e: Exception) {
            Timber.e(e, "Failed to vibrate")
        }
    }

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
