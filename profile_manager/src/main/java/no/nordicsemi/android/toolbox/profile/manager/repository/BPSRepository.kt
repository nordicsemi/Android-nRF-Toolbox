package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.BPSServiceData
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureFeatureData
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.bps.IntermediateCuffPressureData

class BPSRepository {
    private val _data = MutableStateFlow(BPSServiceData())
    val data: StateFlow<BPSServiceData> = _data.asStateFlow()

    fun updateBPSData(bpsData: BloodPressureMeasurementData) {
        _data.update { it.copy(bloodPressureMeasurement = bpsData) }
    }

    fun updateICPData(icpData: IntermediateCuffPressureData) {
        _data.update { it.copy(intermediateCuffPressure = icpData) }
    }

    fun updateBPSFeatureData(bpsFeatureData: BloodPressureFeatureData) {
        _data.update { it.copy(bloodPressureFeature = bpsFeatureData) }
    }

    fun clear() {
        _data.value = BPSServiceData()
    }
}
