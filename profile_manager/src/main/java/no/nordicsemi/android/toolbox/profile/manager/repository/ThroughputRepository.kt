package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.parser.throughput.ThroughputMetrics
import no.nordicsemi.android.toolbox.profile.data.ThroughputInputType
import no.nordicsemi.android.toolbox.profile.data.ThroughputServiceData
import no.nordicsemi.android.toolbox.profile.data.WritingStatus
import no.nordicsemi.android.toolbox.profile.manager.ThroughputManager

object ThroughputRepository {
    private val _dataMap = mutableMapOf<String, MutableStateFlow<ThroughputServiceData>>()
    private val _managers = mutableMapOf<String, ThroughputManager>()

    internal fun registerManager(deviceId: String, manager: ThroughputManager) {
        _managers[deviceId] = manager
    }

    fun getData(deviceId: String): StateFlow<ThroughputServiceData> =
        _dataMap.getOrPut(deviceId) { MutableStateFlow(ThroughputServiceData()) }

    fun updateThroughput(deviceId: String, throughputMetrics: ThroughputMetrics) {
        _dataMap[deviceId]?.update {
            it.copy(throughputData = throughputMetrics)
        }
    }

    suspend fun sendDataToDK(
        deviceId: String,
        writeDataType: ThroughputInputType,
    ) {
        val maxWriteValueLength = _dataMap[deviceId]?.value?.maxWriteValueLength ?: 20
        _managers[deviceId]?.writeRequest(
            maxWriteValueLength = maxWriteValueLength,
            inputType = writeDataType,
        )
    }

    fun updateWriteStatus(deviceId: String, status: WritingStatus) {
        _dataMap[deviceId]?.update { it.copy(writingStatus = status) }
    }

    fun updateMaxWriteValueLength(deviceId: String, mtuSize: Int?) {
        _dataMap[deviceId]?.update { it.copy(maxWriteValueLength = mtuSize) }
    }

    fun clearData(deviceId: String) {
        _dataMap.remove(deviceId)
        _managers.remove(deviceId)
    }

}