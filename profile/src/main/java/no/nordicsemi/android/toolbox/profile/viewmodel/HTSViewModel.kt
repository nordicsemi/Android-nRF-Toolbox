package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.toolbox.profile.manager.repository.HTSRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.data.HTSServiceData
import no.nordicsemi.android.toolbox.profile.data.uiMapper.TemperatureUnit
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

// HTS Profile Events
internal sealed interface HTSEvent {
    data class OnTemperatureUnitSelected(
        val value: TemperatureUnit
    ) : HTSEvent
}

@HiltViewModel
internal class HTSViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    // StateFlow to hold the selected temperature unit
    private val _htsServiceState = MutableStateFlow(HTSServiceData())
    val htsServiceState = _htsServiceState.asStateFlow()
    val address = parameterOf(ProfileDestinationId)

    init {
        observeHtsProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.HTS].
     */
    private fun observeHtsProfile() {
        viewModelScope.launch {
            // update state or emit to UI
            deviceRepository.profileHandlerFlow
                .onEach { mapOfPeripheralProfiles ->
                    mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                        if (peripheral.address == address) {
                            profiles.filter { it.profile == Profile.HTS }
                                .forEach { _ ->
                                    startHTSService(peripheral.address)
                                }
                        }
                    }
                }.launchIn(this)
        }
    }

    /**
     * Starts the HTS service and observes temperature changes.
     *
     * @param address The address of the peripheral device.
     */
    private fun startHTSService(address: String) {
        // Start the HTS service and observe temperature changes
        HTSRepository.getData(address)
            .onEach { htsServiceData ->
                _htsServiceState.value = _htsServiceState.value.copy(
                    data = htsServiceData.data,
                    temperatureUnit = htsServiceData.temperatureUnit,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles events related to the HTS profile.
     *
     * @param event The event to handle.
     */
    fun onEvent(event: HTSEvent) {
        when (event) {
            is HTSEvent.OnTemperatureUnitSelected -> {
                // Handle the temperature unit selection event
                HTSRepository.onTemperatureUnitChange(
                    deviceId = address,
                    unit = event.value
                )
            }
        }
    }
}