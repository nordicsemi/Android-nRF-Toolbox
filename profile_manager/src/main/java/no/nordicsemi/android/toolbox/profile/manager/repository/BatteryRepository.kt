package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.BatteryServiceData

class BatteryRepository {
    private val _data = MutableStateFlow(BatteryServiceData())
    val data: StateFlow<BatteryServiceData> = _data.asStateFlow()

    fun updateBatteryLevel(level: Int) {
        _data.update { it.copy(batteryLevel = level) }
    }

    fun clear() {
        _data.value = BatteryServiceData()
    }
}
