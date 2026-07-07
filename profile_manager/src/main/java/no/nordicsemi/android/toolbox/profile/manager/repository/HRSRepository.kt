package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.HRSServiceData
import no.nordicsemi.android.toolbox.profile.parser.hrs.BodySensorLocation
import no.nordicsemi.android.toolbox.profile.parser.hrs.HRSData

class HRSRepository {
    private val _data = MutableStateFlow(HRSServiceData())
    val data: StateFlow<HRSServiceData> = _data.asStateFlow()

    fun updateHRSData(data: HRSData) {
        _data.update {
            it.copy(
                heartRate = data.heartRate,
                data = it.data + data,
            )
        }
    }

    fun updateBodySensorLocation(location: BodySensorLocation) {
        _data.update { it.copy(bodySensorLocation = location) }
    }

    fun updateZoomIn() {
        _data.update { it.copy(zoomIn = !it.zoomIn) }
    }

    fun clear() {
        _data.value = HRSServiceData()
    }
}
