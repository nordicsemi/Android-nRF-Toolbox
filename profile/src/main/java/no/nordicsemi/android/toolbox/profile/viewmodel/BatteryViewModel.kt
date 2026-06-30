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
import no.nordicsemi.android.toolbox.profile.manager.repository.BatteryRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.BatteryServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

@HiltViewModel
internal class BatteryViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    // StateFlow to hold the selected temperature unit
    private val _batteryServiceState = MutableStateFlow(BatteryServiceData())
    val state = _batteryServiceState.asStateFlow()

    init {
        observeBatteryProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.BATTERY].
     */
    private fun observeBatteryProfile() {
        viewModelScope.launch {
            deviceRepository.profileHandlerFlow
                .onEach { mapOfPeripheralProfiles ->
                    mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                        if (peripheral.address == address) {
                            profiles.filter { it.profile == Profile.BATTERY }
                                .forEach { _ ->
                                    startBatteryService(peripheral.address)
                                }
                        }
                    }
                }
                .launchIn(this)
        }
    }

    /**
     * Starts the Battery Service and observes battery level changes.
     *
     * @param address The address of the peripheral device.
     */
    private fun startBatteryService(address: String) {
        BatteryRepository.getData(address)
            .onEach { batteryServiceState ->
                // Handle the temperature data, e.g., update UI or state
                // This is where you would emit the temperature data to your UI
                _batteryServiceState.value = batteryServiceState
            }
            .launchIn(viewModelScope)
    }
}