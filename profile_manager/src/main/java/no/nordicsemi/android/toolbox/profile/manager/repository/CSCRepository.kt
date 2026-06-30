package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.CSCServiceData
import no.nordicsemi.android.toolbox.profile.parser.csc.CSCData
import no.nordicsemi.android.toolbox.profile.parser.csc.SpeedUnit
import no.nordicsemi.android.toolbox.profile.parser.csc.WheelSize

class CSCRepository {
    private val _data = MutableStateFlow(CSCServiceData())
    val data: StateFlow<CSCServiceData> = _data.asStateFlow()

    val wheelSize: WheelSize
        get() = _data.value.data.wheelSize

    fun onCSCDataChanged(cscData: CSCData) {
        _data.update { it.copy(data = cscData) }
    }

    fun setWheelSize(wheelSize: WheelSize) {
        _data.update { it.copy(data = CSCData(wheelSize = wheelSize)) }
    }

    fun setSpeedUnit(speedUnit: SpeedUnit) {
        _data.update { it.copy(speedUnit = speedUnit) }
    }

    fun clear() {
        _data.value = CSCServiceData()
    }
}
