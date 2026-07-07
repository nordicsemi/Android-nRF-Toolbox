package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.parser.prx.AlarmLevel
import no.nordicsemi.android.toolbox.profile.parser.prx.PRXData

class PRXRepository {
    private val _data = MutableStateFlow(PRXData())
    val data: StateFlow<PRXData> = _data.asStateFlow()

    fun updatePRXData(alarmLevel: AlarmLevel) {
        _data.update { it.copy(localAlarmLevel = alarmLevel) }
    }

    fun updateLinkLossAlarmLevelData(linkLossAlarmLevel: AlarmLevel) {
        _data.update { it.copy(linkLossAlarmLevel = linkLossAlarmLevel) }
    }

    fun clear() {
        _data.value = PRXData()
    }
}
