package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.data.ThroughputInputType
import no.nordicsemi.android.toolbox.profile.manager.ThroughputManager

internal sealed interface ThroughputEvent {
    data class OnWriteData(val writeType: ThroughputInputType) : ThroughputEvent
    data class UpdateMaxWriteValueLength(val maxWriteValueLength: Int? = null) : ThroughputEvent
}

@HiltViewModel(assistedFactory = ThroughputViewModel.Factory::class)
internal class ThroughputViewModel @AssistedInject constructor(
    @Assisted private val manager: ThroughputManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: ThroughputManager): ThroughputViewModel }

    val state = manager.repository.data

    fun onEvent(event: ThroughputEvent) {
        when (event) {
            is ThroughputEvent.OnWriteData -> viewModelScope.launch {
                manager.writeRequest(manager.repository.maxWriteValueLength, event.writeType)
            }

            is ThroughputEvent.UpdateMaxWriteValueLength ->
                manager.repository.updateMaxWriteValueLength(event.maxWriteValueLength)
        }
    }
}
