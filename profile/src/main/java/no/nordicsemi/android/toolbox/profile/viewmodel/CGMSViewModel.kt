package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.manager.CGMManager
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode

internal sealed interface CGMSEvent {
    data class OnWorkingModeSelected(val workingMode: WorkingMode) : CGMSEvent
}

@HiltViewModel(assistedFactory = CGMSViewModel.Factory::class)
internal class CGMSViewModel @AssistedInject constructor(
    @Assisted private val manager: CGMManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: CGMManager): CGMSViewModel }

    val state = manager.repository.data

    fun onEvent(event: CGMSEvent) {
        when (event) {
            is CGMSEvent.OnWorkingModeSelected -> viewModelScope.launch {
                manager.requestRecord(event.workingMode)
            }
        }
    }
}
