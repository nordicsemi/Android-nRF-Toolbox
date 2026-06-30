package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.UARTRecord
import no.nordicsemi.android.toolbox.profile.data.UARTRecordType
import no.nordicsemi.android.toolbox.profile.data.UARTServiceData
import no.nordicsemi.android.toolbox.profile.data.uart.UARTConfiguration
import no.nordicsemi.android.toolbox.profile.data.uart.UARTMacro

class UartRepository {
    private val _data = MutableStateFlow(UARTServiceData())
    val data: StateFlow<UARTServiceData> = _data.asStateFlow()

    val maxWriteLength: Int
        get() = _data.value.maxWriteLength

    fun updateMaxWriteLength(maxWriteLength: Int) {
        _data.update { it.copy(maxWriteLength = maxWriteLength) }
    }

    fun onNewMessageReceived(message: String) {
        _data.update { it.copy(messages = it.messages + UARTRecord(message, UARTRecordType.OUTPUT)) }
    }

    fun onNewMessageSent(message: String) {
        _data.update { it.copy(messages = it.messages + UARTRecord(message, UARTRecordType.INPUT)) }
    }

    fun clear() {
        _data.value = UARTServiceData()
    }

    fun clearOutputItems() {
        _data.update { it.copy(messages = emptyList()) }
    }

    fun deleteConfiguration(configuration: UARTConfiguration) {
        _data.update {
            it.copy(uartViewState = it.uartViewState.copy(configurations = it.uartViewState.configurations - configuration))
        }
    }

    fun addConfiguration(configuration: UARTConfiguration) {
        _data.update {
            val newConfig = configuration.copy(id = it.uartViewState.configurations.size + 1)
            it.copy(uartViewState = it.uartViewState.copy(configurations = it.uartViewState.configurations + newConfig))
        }
    }

    fun updateSelectedConfigurationName(configurationName: String) {
        _data.update {
            it.copy(uartViewState = it.uartViewState.copy(selectedConfigurationName = configurationName))
        }
    }

    fun loadPreviousConfigurations(configuration: List<UARTConfiguration>) {
        _data.update {
            it.copy(uartViewState = it.uartViewState.copy(configurations = configuration))
        }
    }

    fun removeSelectedConfiguration() {
        _data.update {
            it.copy(uartViewState = it.uartViewState.copy(selectedConfigurationName = null))
        }
    }

    fun onEditConfiguration() {
        _data.update {
            it.copy(uartViewState = it.uartViewState.copy(isConfigurationEdited = !it.uartViewState.isConfigurationEdited))
        }
    }

    fun onEditMacro(editPosition: Int?) {
        _data.update {
            it.copy(uartViewState = it.uartViewState.copy(editedPosition = editPosition))
        }
    }

    fun addOrEditMacro(macro: UARTMacro): UARTConfiguration? {
        var newConfig: UARTConfiguration? = null
        _data.update {
            it.uartViewState.selectedConfiguration?.let { selectedConfiguration ->
                val macros = selectedConfiguration.macros.toMutableList().apply {
                    set(it.uartViewState.editedPosition!!, macro)
                }
                newConfig = selectedConfiguration.copy(macros = macros)
                val newConfiguration = it.uartViewState.configurations.map { config ->
                    if (config.id == selectedConfiguration.id) newConfig else config
                }
                it.copy(
                    uartViewState = it.uartViewState.copy(
                        configurations = newConfiguration,
                        editedPosition = null,
                    )
                )
            }!!
        }
        return newConfig
    }

    fun onEditFinished() {
        _data.update {
            it.copy(uartViewState = it.uartViewState.copy(editedPosition = null))
        }
    }

    fun onDeleteMacro() {
        _data.update {
            it.uartViewState.selectedConfiguration?.let { selectedConfiguration ->
                val macros = selectedConfiguration.macros.toMutableList().apply {
                    set(it.uartViewState.editedPosition!!, null)
                }
                val newConfig = selectedConfiguration.copy(macros = macros)
                val newConfiguration = it.uartViewState.configurations.map { config ->
                    if (config.id == selectedConfiguration.id) newConfig else config
                }
                it.copy(
                    uartViewState = it.uartViewState.copy(
                        configurations = newConfiguration,
                        editedPosition = null,
                    )
                )
            }!!
        }
    }
}
