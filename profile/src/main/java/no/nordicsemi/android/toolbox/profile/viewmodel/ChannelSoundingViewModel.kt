package no.nordicsemi.android.toolbox.profile.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.data.ChannelSoundingServiceData
import no.nordicsemi.android.toolbox.profile.data.UpdateRate
import no.nordicsemi.android.toolbox.profile.repository.channelSounding.ChannelSoundingManager
import no.nordicsemi.kotlin.ble.core.BondState
import timber.log.Timber
import no.nordicsemi.android.toolbox.profile.manager.ChannelSoundingManager as RASManager

// Channel Sounding Profile Events
internal sealed interface ChannelSoundingEvent {
    data class RangingUpdateRate(val frequency: UpdateRate) : ChannelSoundingEvent
    data class UpdateInterval(val interval: Int) : ChannelSoundingEvent
    data object RestartRangingSession : ChannelSoundingEvent
}

@HiltViewModel(assistedFactory = ChannelSoundingViewModel.Factory::class)
internal class ChannelSoundingViewModel @AssistedInject constructor(
    @Assisted private val manager: RASManager,
    private val channelSoundingManager: ChannelSoundingManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: RASManager): ChannelSoundingViewModel }

    private val _state =
        MutableStateFlow<Map<String, ChannelSoundingServiceData>>(emptyMap())
    val state = _state.asStateFlow()

    private val collectionJobs = mutableMapOf<String, Job>()

    init {
        observeChannelSoundingProfile()
    }

    /**
     * Waits until the peripheral gets bonded and starts the Channel Sounding service.
     */
    private fun observeChannelSoundingProfile() {
        viewModelScope.launch {
            // Wait until the device is bonded before starting channel sounding.
            manager.peripheral.bondState.first { it == BondState.BONDED }
            startChannelSounding(manager.peripheral.identifier)
        }
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
     * Handles events related to the Channel Sounding profile.
     */
    fun onEvent(event: ChannelSoundingEvent) {
        val targetAddress = manager.peripheral.identifier

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