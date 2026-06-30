package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.CGMRecordWithSequenceNumber
import no.nordicsemi.android.toolbox.profile.data.CGMServiceData
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus

class CGMRepository {
    private val _data = MutableStateFlow(CGMServiceData())
    val data: StateFlow<CGMServiceData> = _data.asStateFlow()

    fun onMeasurementDataReceived(data: List<CGMRecordWithSequenceNumber>) {
        _data.update { it.copy(records = it.records + data) }
    }

    fun updateNewRequestStatus(requestStatus: RequestStatus) {
        _data.update { it.copy(requestStatus = requestStatus) }
    }

    fun updateWorkingMode(mode: WorkingMode) {
        _data.update { it.copy(workingMode = mode) }
    }

    fun clearState() {
        _data.update { it.copy(records = emptyList()) }
    }

    fun clear() {
        _data.value = CGMServiceData()
    }
}
