package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.data.UpdateRate
import no.nordicsemi.android.toolbox.profile.manager.ChannelSoundingManager

// Channel Sounding Profile Events
internal sealed interface ChannelSoundingEvent {
    data class RangingUpdateRate(val frequency: UpdateRate) : ChannelSoundingEvent
    data class UpdateInterval(val interval: Int) : ChannelSoundingEvent
    data object RestartRangingSession : ChannelSoundingEvent
}

@HiltViewModel(assistedFactory = ChannelSoundingViewModel.Factory::class)
internal class ChannelSoundingViewModel @AssistedInject constructor(
    @Assisted private val manager: ChannelSoundingManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(manager: ChannelSoundingManager): ChannelSoundingViewModel
    }

    val state = manager.repository.data

    /**
     * Handles events related to the Channel Sounding profile.
     */
    fun onEvent(event: ChannelSoundingEvent) {
        when (event) {
            is ChannelSoundingEvent.RangingUpdateRate ->
                manager.changeUpdateRate(event.frequency)

            is ChannelSoundingEvent.UpdateInterval ->
                manager.repository.updateInterval(event.interval)

            ChannelSoundingEvent.RestartRangingSession ->
                manager.restartRangingSession()
        }
    }
}
