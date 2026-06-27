package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode
import no.nordicsemi.android.toolbox.profile.manager.repository.GLSRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.data.GLSServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

// GLSProfile Events
internal sealed interface GLSEvent {
    data class OnWorkingModeSelected(
        val workingMode: WorkingMode
    ) : GLSEvent

}

@HiltViewModel
internal class GLSViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    val address = parameterOf(ProfileDestinationId)
    private val _glsState = MutableStateFlow(GLSServiceData())
    val glsState = _glsState.asStateFlow()

    init {
        observeGLSProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.GLS].
     */
    private fun observeGLSProfile() {
        deviceRepository.profileHandlerFlow
            .onEach { mapOfPeripheralProfiles ->
                mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                    if (peripheral.address == address) {
                        profiles.filter { it.profile == Profile.GLS }
                            .forEach { _ ->
                                startGLSService(peripheral.address)
                            }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the GLS service and observes glucose profile data changes.
     */
    private fun startGLSService(address: String) {
        GLSRepository.getData(address)
            .onEach {
                _glsState.value = _glsState.value.copy(
                    profile = it.profile,
                    records = it.records,
                    requestStatus = it.requestStatus,
                    workingMode = it.workingMode,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles events related to the GLS profile.
     */
    fun onEvent(event: GLSEvent) {
        when (event) {
            is GLSEvent.OnWorkingModeSelected -> viewModelScope.launch {
                GLSRepository.requestRecord(address, event.workingMode)
            }
        }
    }
}