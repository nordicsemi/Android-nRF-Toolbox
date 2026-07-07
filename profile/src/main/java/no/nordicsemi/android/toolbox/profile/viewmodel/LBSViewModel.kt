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
}

@HiltViewModel(assistedFactory = LBSViewModel.Factory::class)
internal class LBSViewModel @AssistedInject constructor(
    @Assisted private val manager: LBSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: LBSManager): LBSViewModel }

    val state = manager.repository.data

    fun onEvent(event: LBSEvent) {
        when (event) {
            is LBSEvent.OnLedStateChanged -> viewModelScope.launch {
                manager.writeLED(event.value)
            }

            is LBSEvent.OnButtonStateChanged ->
                manager.repository.updateButtonState(event.value)
        }
    }
}
