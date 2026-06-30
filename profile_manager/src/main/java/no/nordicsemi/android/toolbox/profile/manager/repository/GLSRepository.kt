package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.GLSServiceData
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GLSMeasurementContext
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GLSRecord
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus

class GLSRepository {
    private val _data = MutableStateFlow(GLSServiceData())
    val data: StateFlow<GLSServiceData> = _data.asStateFlow()

    fun updateNewRecord(record: GLSRecord) {
        val records = _data.value.records.toMutableMap()
        records[record] = null
        _data.update { it.copy(records = records.toMap()) }
    }

    fun updateWithNewContext(context: GLSMeasurementContext) {
        val records = _data.value.records.toMutableMap()
        records.keys.firstOrNull { it.sequenceNumber == context.sequenceNumber }
            ?.let { records[it] = context }
        _data.update { it.copy(records = records.toMap()) }
    }

    fun updateNewRequestStatus(requestStatus: RequestStatus) {
        _data.update { it.copy(requestStatus = requestStatus) }
    }

    fun updateWorkingMode(mode: WorkingMode) {
        _data.update { it.copy(workingMode = mode) }
    }

    fun clearState() {
        _data.update { it.copy(records = mapOf(), requestStatus = RequestStatus.IDLE) }
    }

    fun clear() {
        _data.value = GLSServiceData()
    }
}
