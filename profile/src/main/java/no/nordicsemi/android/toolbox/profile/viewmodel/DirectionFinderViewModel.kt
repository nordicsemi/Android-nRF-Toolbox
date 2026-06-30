package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.manager.DDFSManager
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointMode

internal sealed interface DFSEvent {
    data object OnAvailableDistanceModeRequest : DFSEvent
    data object OnCheckDistanceModeRequest : DFSEvent
    data class OnRangeChangedEvent(val range: IntRange) : DFSEvent
    data class OnDistanceModeSelected(val mode: ControlPointMode) : DFSEvent
    data class OnBluetoothDeviceSelected(val device: PeripheralBluetoothAddress) : DFSEvent
}

@HiltViewModel(assistedFactory = DirectionFinderViewModel.Factory::class)
internal class DirectionFinderViewModel @AssistedInject constructor(
    @Assisted private val manager: DDFSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: DDFSManager): DirectionFinderViewModel }

    val state = manager.repository.data

    fun onEvent(event: DFSEvent) {
        when (event) {
            DFSEvent.OnAvailableDistanceModeRequest -> viewModelScope.launch {
                manager.checkAvailableFeatures()
            }

            DFSEvent.OnCheckDistanceModeRequest -> viewModelScope.launch {
                manager.checkForCurrentDistanceMode()
            }

            is DFSEvent.OnRangeChangedEvent ->
                manager.repository.updateDistanceRange(event.range)

            is DFSEvent.OnDistanceModeSelected -> viewModelScope.launch {
                manager.enableDistanceMode(event.mode)
            }

            is DFSEvent.OnBluetoothDeviceSelected ->
                manager.repository.updateSelectedDevice(event.device)
        }
    }
}
