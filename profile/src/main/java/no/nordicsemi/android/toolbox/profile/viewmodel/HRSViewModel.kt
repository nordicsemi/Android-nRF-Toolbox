package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.manager.HRSManager

internal sealed interface HRSEvent {
    data object SwitchZoomEvent : HRSEvent
}

@HiltViewModel(assistedFactory = HRSViewModel.Factory::class)
internal class HRSViewModel @AssistedInject constructor(
    @Assisted private val manager: HRSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: HRSManager): HRSViewModel }

    val state = manager.repository.data

    fun onEvent(event: HRSEvent) {
        when (event) {
            HRSEvent.SwitchZoomEvent -> manager.repository.updateZoomIn()
        }
    }
}
