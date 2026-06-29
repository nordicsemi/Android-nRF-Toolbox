package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.DFUServiceData
import no.nordicsemi.android.toolbox.profile.data.DFUsAvailable

object DFURepository {
    private val _dataMap = mutableMapOf<String, MutableStateFlow<DFUServiceData>>()

    fun getData(deviceId: String): Flow<DFUServiceData> =
        _dataMap.getOrPut(deviceId) { MutableStateFlow(DFUServiceData()) }

    fun updateAppName(deviceId: String, appName: DFUsAvailable) {
        _dataMap[deviceId]?.let {
            it.update { dfuServiceData ->
                dfuServiceData.copy(dfuAppName = appName)
            }
        } ?: run {
            _dataMap[deviceId] = MutableStateFlow(DFUServiceData(dfuAppName = appName))
        }
    }

    fun clear(deviceId: String) {
        _dataMap.remove(deviceId)
    }

}