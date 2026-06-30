package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.data.uiMapper.TemperatureUnit
import no.nordicsemi.android.toolbox.profile.manager.HTSManager

internal sealed interface HTSEvent {
    data class OnTemperatureUnitSelected(val value: TemperatureUnit) : HTSEvent
}

@HiltViewModel(assistedFactory = HTSViewModel.Factory::class)
internal class HTSViewModel @AssistedInject constructor(
    @Assisted private val manager: HTSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: HTSManager): HTSViewModel }

    val state = manager.repository.data

    fun onEvent(event: HTSEvent) {
        when (event) {
            is HTSEvent.OnTemperatureUnitSelected ->
                manager.repository.onTemperatureUnitChange(event.value)
        }
    }
}
