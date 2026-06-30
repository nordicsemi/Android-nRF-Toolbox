package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.DFUServiceData
import no.nordicsemi.android.toolbox.profile.data.DFUsAvailable

class DFURepository {
    private val _data = MutableStateFlow(DFUServiceData())
    val data: StateFlow<DFUServiceData> = _data.asStateFlow()

    fun updateAppName(appName: DFUsAvailable) {
        _data.update { it.copy(dfuAppName = appName) }
    }

    fun clear() {
        _data.value = DFUServiceData()
    }
}
