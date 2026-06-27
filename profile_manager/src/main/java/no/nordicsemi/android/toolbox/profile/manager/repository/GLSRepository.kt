package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GLSMeasurementContext
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GLSRecord
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus
import no.nordicsemi.android.toolbox.profile.data.GLSServiceData
import no.nordicsemi.android.toolbox.profile.manager.GLSManager

object GLSRepository {
    private val _dataMap = mutableMapOf<String, MutableStateFlow<GLSServiceData>>()

    fun getData(deviceId: String): StateFlow<GLSServiceData> = _dataMap.getOrPut(deviceId) {
        MutableStateFlow(GLSServiceData())
    }

    fun updateNewRecord(deviceId: String, record: GLSRecord) {
        val records = _dataMap[deviceId]?.value?.records?.toMutableMap()
        records?.set(record, null)
        if (records != null) {
            _dataMap[deviceId]?.update {
                it.copy(
                    records = records.toMap()
                )
            }
        }
    }

    fun updateWithNewContext(deviceId: String, context: GLSMeasurementContext) {
        val records = _dataMap[deviceId]?.value?.records?.toMutableMap()
        records?.keys?.firstOrNull { it.sequenceNumber == context.sequenceNumber }?.let {
            records[it] = context
        }
        if (records != null) {
            _dataMap[deviceId]?.update {
                it.copy(
                    records = records.toMap()
                )
            }
        }
    }

    suspend fun requestRecord(deviceId: String, workingMode: WorkingMode) {
        clearState(deviceId)
        updateNewRequestStatus(deviceId, RequestStatus.PENDING)
        _dataMap[deviceId]?.update { it.copy(workingMode = workingMode) }
        GLSManager.requestRecord(deviceId, workingMode)
    }

    fun updateNewRequestStatus(deviceId: String, requestStatus: RequestStatus) {
        _dataMap[deviceId]?.update { it.copy(requestStatus = requestStatus) }
    }

    private fun clearState(deviceId: String) {
        _dataMap[deviceId]?.update {
            it.copy(
                records = mapOf(),
                requestStatus = RequestStatus.IDLE
            )
        }
    }

    fun clear(deviceId: String) {
        _dataMap.remove(deviceId)
    }

}

