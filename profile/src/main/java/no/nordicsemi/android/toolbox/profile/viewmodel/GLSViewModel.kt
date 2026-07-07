package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.manager.GLSManager
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode

internal sealed interface GLSEvent {
    data class OnWorkingModeSelected(val workingMode: WorkingMode) : GLSEvent
}

@HiltViewModel(assistedFactory = GLSViewModel.Factory::class)
internal class GLSViewModel @AssistedInject constructor(
    @Assisted private val manager: GLSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: GLSManager): GLSViewModel }

    val state = manager.repository.data

    fun onEvent(event: GLSEvent) {
        when (event) {
            is GLSEvent.OnWorkingModeSelected -> viewModelScope.launch {
                manager.requestRecord(event.workingMode)
            }
        }
    }
}
