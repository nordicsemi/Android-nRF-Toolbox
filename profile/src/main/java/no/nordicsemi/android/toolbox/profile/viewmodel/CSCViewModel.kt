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
import no.nordicsemi.android.toolbox.profile.parser.csc.SpeedUnit
import no.nordicsemi.android.toolbox.profile.parser.csc.WheelSize
import no.nordicsemi.android.toolbox.profile.manager.repository.CSCRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.CSCServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

// CSC Profile Events
internal sealed interface CSCEvent {
    data class OnWheelSizeSelected(val wheelSize: WheelSize) : CSCEvent
    data class OnSelectedSpeedUnitSelected(val selectedSpeedUnit: SpeedUnit) : CSCEvent
}

@HiltViewModel
internal class CSCViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    private val _state = MutableStateFlow(CSCServiceData())
    val state = _state.asStateFlow()

    init {
        observeCSCProfile()
    }

    private fun observeCSCProfile() {
        viewModelScope.launch {
            // update state or emit to UI
            deviceRepository.profileHandlerFlow
                .onEach { mapOfPeripheralProfiles ->
                    mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                        if (peripheral.address == address) {
                            profiles.filter { it.profile == Profile.CSC }
                                .forEach { _ ->
                                    startCSCService(peripheral.address)
                                }
                        }
                    }
                }
                .launchIn(this)
        }
    }

    private fun startCSCService(address: String) {
        // Start the LBS service and observe location changes
        CSCRepository.getData(address)
            .onEach {
                _state.value = _state.value.copy(
                    profile = it.profile,
                    data = it.data,
                    speedUnit = it.speedUnit
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: CSCEvent) {
        when (event) {
            is CSCEvent.OnWheelSizeSelected ->
                CSCRepository.setWheelSize(address, event.wheelSize)

            is CSCEvent.OnSelectedSpeedUnitSelected ->
                CSCRepository.setSpeedUnit(address, event.selectedSpeedUnit)
        }
    }
}