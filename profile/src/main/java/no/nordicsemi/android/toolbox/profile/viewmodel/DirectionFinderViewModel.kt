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
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.data.DFSServiceData
import no.nordicsemi.android.toolbox.profile.manager.repository.DFSRepository
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointMode
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

internal sealed interface DFSEvent {
    data object OnAvailableDistanceModeRequest : DFSEvent
    data object OnCheckDistanceModeRequest : DFSEvent
    data class OnRangeChangedEvent(val range: IntRange) : DFSEvent
    data class OnDistanceModeSelected(val mode: ControlPointMode) : DFSEvent
    data class OnBluetoothDeviceSelected(val device: PeripheralBluetoothAddress) : DFSEvent
}

@HiltViewModel
internal class DirectionFinderViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val _dfsState = MutableStateFlow(DFSServiceData())
    val dfsState = _dfsState.asStateFlow()
    private val address = parameterOf(ProfileDestinationId)

    init {
        observeDFSProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.DFS].
     */
    private fun observeDFSProfile() {
        deviceRepository.profileHandlerFlow
            .onEach { mapOfPeripheralProfiles ->
                mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                    if (peripheral.address == address) {
                        profiles.filter { it.profile == Profile.DFS }
                            .forEach { _ ->
                                startDFSService()
                            }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the DFS service and observes direction finder profile data changes.
     */
    private fun startDFSService() {
        DFSRepository.getData(address)
            .onEach {
                _dfsState.value = _dfsState.value.copy(
                    requestStatus = it.requestStatus,
                    data = it.data,
                    ddfFeature = it.ddfFeature,
                    selectedDevice = it.selectedDevice,
                    distanceRange = it.distanceRange,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles events related to the Direction Finder Service (DFS).
     */
    fun onEvent(event: DFSEvent) {
        when (event) {
            DFSEvent.OnAvailableDistanceModeRequest -> viewModelScope.launch {
                DFSRepository.checkAvailableFeatures(address)
            }

            DFSEvent.OnCheckDistanceModeRequest -> viewModelScope.launch {
                DFSRepository.checkCurrentDistanceMode(address)
            }

            is DFSEvent.OnRangeChangedEvent -> {
                DFSRepository.updateDistanceRange(address, event.range)
            }

            is DFSEvent.OnDistanceModeSelected -> {
                viewModelScope.launch {
                    DFSRepository.enableDistanceMode(address, event.mode)
                }
            }

            is DFSEvent.OnBluetoothDeviceSelected -> DFSRepository.updateSelectedDevice(
                address,
                event.device
            )
        }
    }
}