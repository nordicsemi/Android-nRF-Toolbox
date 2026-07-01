package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.RSCSServiceData
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCFeatureData
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSData
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSSettingsUnit

class RSCSRepository {
    private val _data = MutableStateFlow(RSCSServiceData())
    val data: StateFlow<RSCSServiceData> = _data.asStateFlow()

    fun onRSCSDataChanged(data: RSCSData) {
        _data.update { it.copy(data = data) }
    }

    fun updateUnitSettings(rscsUnitSettings: RSCSSettingsUnit) {
        _data.update { it.copy(unit = rscsUnitSettings) }
    }

    fun updateRSCSFeatureData(feature: RSCFeatureData) {
        _data.update { it.copy(feature = feature) }
    }

    fun clear() {
        _data.value = RSCSServiceData()
    }
}
