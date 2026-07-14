package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.ChannelSoundingServiceData
import no.nordicsemi.android.toolbox.profile.data.RangingSessionAction
import no.nordicsemi.android.toolbox.profile.data.UpdateRate

class ChannelSoundingRepository {
    private val _data = MutableStateFlow(ChannelSoundingServiceData())
    val data: StateFlow<ChannelSoundingServiceData> = _data.asStateFlow()

    fun updateSessionAction(action: RangingSessionAction) {
        _data.update { it.copy(rangingSessionAction = action) }
    }

    fun updateRate(rate: UpdateRate) {
        _data.update { it.copy(updateRate = rate) }
    }

    fun updateInterval(interval: Int) {
        _data.update { it.copy(interval = interval) }
    }

    fun clear() {
        _data.value = ChannelSoundingServiceData()
    }
}
