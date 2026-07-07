package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.ThroughputServiceData
import no.nordicsemi.android.toolbox.profile.data.WritingStatus
import no.nordicsemi.android.toolbox.profile.parser.throughput.ThroughputMetrics

class ThroughputRepository {
    private val _data = MutableStateFlow(ThroughputServiceData())
    val data: StateFlow<ThroughputServiceData> = _data.asStateFlow()

    val maxWriteValueLength: Int
        get() = _data.value.maxWriteValueLength ?: 20

    fun updateThroughput(throughputMetrics: ThroughputMetrics) {
        _data.update { it.copy(throughputData = throughputMetrics) }
    }

    fun updateWriteStatus(status: WritingStatus) {
        _data.update { it.copy(writingStatus = status) }
    }

    fun updateMaxWriteValueLength(mtuSize: Int?) {
        _data.update { it.copy(maxWriteValueLength = mtuSize) }
    }

    fun clearData() {
        _data.value = ThroughputServiceData()
    }
}
