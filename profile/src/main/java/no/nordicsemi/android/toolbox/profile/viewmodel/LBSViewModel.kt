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
import no.nordicsemi.android.toolbox.profile.manager.repository.LBSRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.LBSServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

// LBS Profile Events
internal sealed interface LBSEvent {
    data class OnLedStateChanged(
        val value: Boolean
    ) : LBSEvent

    data class OnButtonStateChanged(
        val value: Boolean
    ) : LBSEvent
}

@HiltViewModel
internal class LBSViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    private val _state = MutableStateFlow(LBSServiceData())
    val state = _state.asStateFlow()

    init {
        observeLbsProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.LBS].
     */
    private fun observeLbsProfile() {
        viewModelScope.launch {
            // update state or emit to UI
            deviceRepository.profileHandlerFlow
                .onEach { mapOfPeripheralProfiles ->
                    mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                        if (peripheral.address == address) {
                            profiles.filter { it.profile == Profile.LBS }
                                .forEach { _ ->
                                    startLBSService(peripheral.address)
                                }
                        }
                    }
                }
                .launchIn(this)
        }
    }

    /**
     * Starts the LBS service and observes location changes.
     */
    private fun startLBSService(address: String) {
        // Start the LBS service and observe location changes
        LBSRepository.getData(address)
            .onEach {
                _state.value = _state.value.copy(
                    profile = it.profile,
                    data = it.data,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles events related to the LBS profile.
     */
    fun onEvent(event: LBSEvent) {
        when (event) {
            is LBSEvent.OnLedStateChanged -> {
                // Handle LED state change
                viewModelScope.launch {
                    LBSRepository.writeToBlinkyLED(address, event.value)
                }
            }

            is LBSEvent.OnButtonStateChanged -> {
                // Handle button state change
                LBSRepository.updateButtonState(
                    deviceId = address,
                    buttonState = event.value
                )
            }
        }
    }
}