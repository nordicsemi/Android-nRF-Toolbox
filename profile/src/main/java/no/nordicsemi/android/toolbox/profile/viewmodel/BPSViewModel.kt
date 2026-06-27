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
import no.nordicsemi.android.toolbox.profile.manager.repository.BPSRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.data.BPSServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

@HiltViewModel
internal class BPSViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    // StateFlow to hold the selected temperature unit
    private val _bpsServiceState = MutableStateFlow(BPSServiceData())
    val bpsServiceState = _bpsServiceState.asStateFlow()
    private val address = parameterOf(ProfileDestinationId)

    init {
        observeBPSProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.BPS].
     */
    private fun observeBPSProfile() {
        viewModelScope.launch {
            // update state or emit to UI
            deviceRepository.profileHandlerFlow
                .onEach { mapOfPeripheralProfiles ->
                    mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                        if (peripheral.address == address) {
                            profiles.filter { it.profile == Profile.BPS }
                                .forEach { _ ->
                                    startBPSService(peripheral.address)
                                }
                        }
                    }
                }
                .launchIn(this)
        }
    }

    /**
     * Starts the BPS service for the given address and updates the state with the received data.
     */
    private fun startBPSService(address: String) {
        BPSRepository.getData(address)
            .onEach {
                _bpsServiceState.value = _bpsServiceState.value.copy(
                    profile = it.profile,
                    bloodPressureMeasurement = it.bloodPressureMeasurement,
                    intermediateCuffPressure = it.intermediateCuffPressure,
                    bloodPressureFeature = it.bloodPressureFeature,
                )
            }
            .launchIn(viewModelScope)
    }
}
