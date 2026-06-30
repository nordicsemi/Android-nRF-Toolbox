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
import no.nordicsemi.android.toolbox.profile.manager.repository.ThroughputRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.ThroughputInputType
import no.nordicsemi.android.toolbox.profile.data.ThroughputServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

// Throughput events.
internal sealed interface ThroughputEvent {
    data class OnWriteData(
        val writeType: ThroughputInputType,
    ) : ThroughputEvent

    data class UpdateMaxWriteValueLength(
        val maxWriteValueLength: Int? = null,
    ) : ThroughputEvent

}

@HiltViewModel
internal class ThroughputViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    private val _state = MutableStateFlow(ThroughputServiceData())
    val state = _state.asStateFlow()

    init {
        observeThroughputProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.THROUGHPUT].
     */
    private fun observeThroughputProfile() {
        deviceRepository.profileHandlerFlow
            .onEach { mapOfPeripheralProfiles ->
                mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                    if (peripheral.address == address) {
                        profiles.filter { it.profile == Profile.THROUGHPUT }
                            .forEach { _ ->
                                startThroughputService(peripheral.address)
                            }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the Throughput service and observes throughput data changes.
     */
    private fun startThroughputService(address: String) {
        ThroughputRepository.getData(address)
            .onEach {
                _state.value = _state.value.copy(
                    profile = it.profile,
                    throughputData = it.throughputData,
                    writingStatus = it.writingStatus,
                    maxWriteValueLength = it.maxWriteValueLength
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles events related to the Throughput profile.
     */
    fun onEvent(event: ThroughputEvent) {
        when (event) {
            is ThroughputEvent.OnWriteData -> viewModelScope.launch {
                ThroughputRepository.sendDataToDK(address, event.writeType)
            }

            is ThroughputEvent.UpdateMaxWriteValueLength ->
                ThroughputRepository.updateMaxWriteValueLength(
                deviceId = address,
                mtuSize = event.maxWriteValueLength
            )
        }
    }
}