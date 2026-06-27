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
import no.nordicsemi.android.toolbox.profile.manager.repository.CGMRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.data.CGMServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

// CGMS Profile Events
internal sealed interface CGMSEvent {
    data class OnWorkingModeSelected(
        val workingMode: WorkingMode
    ) : CGMSEvent
}

@HiltViewModel
internal class CGMSViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    // StateFlow to hold the selected temperature unit
    private val _cgmsServiceState = MutableStateFlow(CGMServiceData())
    val channelSoundingState = _cgmsServiceState.asStateFlow()

    private val address = parameterOf(ProfileDestinationId)

    init {
        observeCGMSProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.CGM].
     */
    private fun observeCGMSProfile() {
        viewModelScope.launch {
            // update state or emit to UI
            deviceRepository.profileHandlerFlow
                .onEach { mapOfPeripheralProfiles ->
                    mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                        if (peripheral.address == address) {
                            profiles.filter { it.profile == Profile.CGM }
                                .forEach { _ ->
                                    startCGMSService(peripheral.address)
                                }
                        }
                    }
                }
                .launchIn(this)
        }
    }

    /**
     * Starts the CGMS service and observes CGMS profile data changes.
     */
    private fun startCGMSService(address: String) {
        CGMRepository.getData(address)
            .onEach {
                _cgmsServiceState.value = _cgmsServiceState.value.copy(
                    profile = it.profile,
                    records = it.records,
                    requestStatus = it.requestStatus,
                    workingMode = it.workingMode,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles events related to the CGMS profile.
     */
    fun onEvent(event: CGMSEvent) {
        when (event) {
            is CGMSEvent.OnWorkingModeSelected -> viewModelScope.launch {
                // Handle the working mode selection event
                CGMRepository.requestRecord(
                    deviceId = address,
                    workingMode = event.workingMode
                )
            }
        }
    }

}