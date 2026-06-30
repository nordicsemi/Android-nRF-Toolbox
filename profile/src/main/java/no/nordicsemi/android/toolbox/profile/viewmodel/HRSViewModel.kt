package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.toolbox.profile.manager.repository.HRSRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.HRSServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

// Heart Rate Service (HRS) Profile Events
internal sealed interface HRSEvent {
    data object SwitchZoomEvent : HRSEvent
}

@HiltViewModel
internal class HRSViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    private val _state = MutableStateFlow(HRSServiceData())
    val state = _state.asStateFlow()

    init {
        observeHRSProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.HRS].
     */
    private fun observeHRSProfile() {
        deviceRepository.profileHandlerFlow
            .onEach { mapOfPeripheralProfiles ->
                mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                    if (peripheral.address == address) {
                        profiles.filter { it.profile == Profile.HRS }
                            .forEach { _ ->
                                startHRSService(peripheral.address)
                            }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the HRS service and observes heart rate data changes.
     */
    private fun startHRSService(address: String) {
        HRSRepository.getData(address)
            .onEach {
                _state.value = _state.value.copy(
                    profile = it.profile,
                    heartRate = it.heartRate,
                    data = it.data,
                    bodySensorLocation = it.bodySensorLocation,
                    zoomIn = it.zoomIn,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles events related to the HRS profile.
     */
    fun onEvent(event: HRSEvent) {
        when (event) {
            HRSEvent.SwitchZoomEvent -> HRSRepository.updateZoomIn(address)
        }
    }
}