package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.HTSServiceData
import no.nordicsemi.android.toolbox.profile.data.uiMapper.TemperatureUnit
import no.nordicsemi.android.toolbox.profile.parser.hts.HTSData

class HTSRepository {
    private val _data = MutableStateFlow(HTSServiceData())
    val data: StateFlow<HTSServiceData> = _data.asStateFlow()

    fun updateHTSData(data: HTSData) {
        _data.update { it.copy(data = data) }
    }

    fun onTemperatureUnitChange(unit: TemperatureUnit) {
        _data.update { it.copy(temperatureUnit = unit) }
    }

    fun clear() {
        _data.value = HTSServiceData()
    }
}
