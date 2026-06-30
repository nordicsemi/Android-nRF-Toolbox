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
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSSettingsUnit
import no.nordicsemi.android.toolbox.profile.manager.repository.RSCSRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.RSCSServiceData
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import javax.inject.Inject

// RSCS Profile Events
internal sealed interface RSCSEvent {
    data class OnSelectedSpeedUnitSelected(val rscsSettingsUnit: RSCSSettingsUnit) : RSCSEvent
}

@HiltViewModel
internal class RSCSViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    private val _state = MutableStateFlow(RSCSServiceData())
    val state = _state.asStateFlow()

    init {
        observeRSCSProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.RSCS].
     */
    private fun observeRSCSProfile() {
        deviceRepository.profileHandlerFlow
            .onEach { mapOfPeripheralProfiles ->
                mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                    if (peripheral.address == address) {
                        profiles.filter { it.profile == Profile.RSCS }
                            .forEach { _ ->
                                startRSCSService(peripheral.address)
                            }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the RSCS service and observes running speed, cadence, and other data changes.
     */
    private fun startRSCSService(address: String) {
        RSCSRepository.getData(address)
            .onEach {
                _state.value = _state.value.copy(
                    profile = it.profile,
                    data = it.data,
                    unit = it.unit,
                    feature = it.feature,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles events related to the RSCS profile.
     */
    fun onEvent(event: RSCSEvent) {
        when (event) {
            is RSCSEvent.OnSelectedSpeedUnitSelected -> {
                RSCSRepository.updateUnitSettings(address, event.rscsSettingsUnit)
            }
        }
    }
}