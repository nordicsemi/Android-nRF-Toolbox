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
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.DFUServiceData
import no.nordicsemi.android.toolbox.profile.manager.repository.DFURepository
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

@HiltViewModel
internal class DFUViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    // StateFlow to hold the selected temperature unit
    private val _state = MutableStateFlow(DFUServiceData())
    val state = _state.asStateFlow()

    init {
        observeDFUProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.DFU].
     */
    private fun observeDFUProfile() {
        deviceRepository.profileHandlerFlow
            .onEach { mapOfPeripheralProfiles ->
                mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                    if (peripheral.address == address) {
                        profiles.filter { it.profile == Profile.DFU }
                            .forEach { _ ->
                                startDFUService(peripheral.address)
                            }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the DFU Service and observes data changes.
     *
     * @param address The address of the peripheral device.
     */
    private fun startDFUService(address: String) {
        DFURepository.getData(address)
            .onEach { dFUServiceData ->
                _state.value = _state.value.copy(
                    profile = dFUServiceData.profile,
                    dfuAppName = dFUServiceData.dfuAppName
                )
            }
            .launchIn(viewModelScope)
    }

}