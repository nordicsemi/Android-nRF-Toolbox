package no.nordicsemi.android.toolbox.profile.viewmodel

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.ChannelSoundingServiceData
import no.nordicsemi.android.toolbox.profile.data.UpdateRate
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import no.nordicsemi.android.toolbox.profile.repository.channelSounding.ChannelSoundingManager
import no.nordicsemi.kotlin.ble.core.BondState
import timber.log.Timber
import javax.inject.Inject

// Channel Sounding Profile Events
internal sealed interface ChannelSoundingEvent {
    data class RangingUpdateRate(val frequency: UpdateRate) : ChannelSoundingEvent
    data class UpdateInterval(val interval: Int) : ChannelSoundingEvent
    data object RestartRangingSession : ChannelSoundingEvent
}

@HiltViewModel
internal class ChannelSoundingViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
    private val channelSoundingManager: ChannelSoundingManager,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    private val _state =
        MutableStateFlow<Map<String, ChannelSoundingServiceData>>(emptyMap())
    val state = _state.asStateFlow()

    private val collectionJobs = mutableMapOf<String, Job>()

    init {
        observeChannelSoundingProfile()
    }

    /**
     * Observes the [DeviceRepository.profileHandlerFlow] from the [deviceRepository] that contains [Profile.CHANNEL_SOUNDING].
     */
    private fun observeChannelSoundingProfile() {
        deviceRepository.profileHandlerFlow
            .onEach { mapOfPeripheralProfiles ->
                mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                    if (peripheral.address == address) {
                        profiles.filter { it.profile == Profile.CHANNEL_SOUNDING }
                            .forEach { _ ->
                                viewModelScope.launch {
                                    peripheral.bondState.first { it == BondState.BONDED }
                                    // Wait until the device is bonded before starting channel sounding
                                    startChannelSounding(peripheral.address)
                                }
                            }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Ensures we listen to the data updates for a specific device exactly once.
     */
    private fun observeDeviceData(deviceAddress: String) {
        if (collectionJobs.containsKey(deviceAddress)) return // Already observing this device

        collectionJobs[deviceAddress] = channelSoundingManager.getData(deviceAddress)
            .onEach { incomingData ->
                val currentMap = _state.value
                val existingData = currentMap[deviceAddress] ?: ChannelSoundingServiceData()
                val updatedData = existingData.copy(
                    profile = incomingData.profile,
                    updateRate = incomingData.updateRate,
                    rangingSessionAction = incomingData.rangingSessionAction,
                )
                _state.value = currentMap + (deviceAddress to updatedData)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the Channel Sounding service and observes channel sounding profile data changes.
     */
    private fun startChannelSounding(deviceAddress: String, rate: UpdateRate = UpdateRate.NORMAL) {
        observeDeviceData(deviceAddress)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            try {
                val currentDeviceData = _state.value[deviceAddress]
                if (currentDeviceData != null && currentDeviceData.updateRate != rate) {
                    channelSoundingManager.closeSession(deviceAddress) {
                        channelSoundingManager.startRangingMeasurement(deviceAddress, rate)
                    }
                } else {
                    channelSoundingManager.startRangingMeasurement(deviceAddress, rate)
                }
            } catch (e: Exception) {
                Timber.e("${e.message}")
            }
        } else {
            Timber.d("Channel Sounding is not available in this Android version.")
        }
    }

    /**
     * Handles events related to the Channel Sounding profile.
     */
    fun onEvent(event: ChannelSoundingEvent) {
        val targetAddress = address

        when (event) {
            is ChannelSoundingEvent.RangingUpdateRate -> {
                channelSoundingManager.updateRangingRate(targetAddress, event.frequency)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    try {
                        val currentDeviceData = _state.value[targetAddress]
                        if (currentDeviceData?.updateRate != event.frequency) {
                            channelSoundingManager.closeSession(targetAddress) {
                                channelSoundingManager.startRangingMeasurement(
                                    targetAddress,
                                    event.frequency
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("Error closing session: ${e.message}")
                    }
                }
            }

            is ChannelSoundingEvent.UpdateInterval -> {
                channelSoundingManager.updateIntervalRate(targetAddress, event.interval)
            }

            ChannelSoundingEvent.RestartRangingSession -> {
                // Restart the ranging session with the current update rate
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    try {
                        channelSoundingManager.closeSession(targetAddress) {
                            val deviceData = _state.value[targetAddress]
                            val targetRate = deviceData?.updateRate ?: UpdateRate.NORMAL
                            channelSoundingManager.startRangingMeasurement(
                                targetAddress,
                                targetRate
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e("Error closing session: ${e.message}")
                    }
                }
            }
        }
    }
}