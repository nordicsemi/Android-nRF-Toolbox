package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.LBSServiceData

class LBSRepository {
    private val _data = MutableStateFlow(LBSServiceData())
    val data: StateFlow<LBSServiceData> = _data.asStateFlow()

    fun updateLedState(ledState: Boolean) {
        _data.update { it.copy(data = it.data.copy(ledState = ledState)) }
    }

    fun updateButtonState(buttonState: Boolean) {
        _data.update { it.copy(data = it.data.copy(buttonState = buttonState)) }
    }

    fun clear() {
        _data.value = LBSServiceData()
    }
}
