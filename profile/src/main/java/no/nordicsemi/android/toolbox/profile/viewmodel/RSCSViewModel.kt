package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.manager.RSCSManager
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSSettingsUnit

internal sealed interface RSCSEvent {
    data class OnSelectedSpeedUnitSelected(val rscsSettingsUnit: RSCSSettingsUnit) : RSCSEvent
}

@HiltViewModel(assistedFactory = RSCSViewModel.Factory::class)
internal class RSCSViewModel @AssistedInject constructor(
    @Assisted private val manager: RSCSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: RSCSManager): RSCSViewModel }

    val state = manager.repository.data

    fun onEvent(event: RSCSEvent) {
        when (event) {
            is RSCSEvent.OnSelectedSpeedUnitSelected ->
                manager.repository.updateUnitSettings(event.rscsSettingsUnit)
        }
    }
}
