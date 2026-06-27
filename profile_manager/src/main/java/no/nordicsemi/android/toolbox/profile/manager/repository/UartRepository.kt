package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.UARTRecord
import no.nordicsemi.android.toolbox.profile.data.UARTRecordType
import no.nordicsemi.android.toolbox.profile.data.UARTServiceData
import no.nordicsemi.android.toolbox.profile.data.uart.MacroEol
import no.nordicsemi.android.toolbox.profile.data.uart.UARTConfiguration
import no.nordicsemi.android.toolbox.profile.data.uart.UARTMacro
import no.nordicsemi.android.toolbox.profile.data.uart.parseWithNewLineChar
import no.nordicsemi.android.toolbox.profile.manager.UARTManager

object UartRepository {
    private val _dataMap = mutableMapOf<String, MutableStateFlow<UARTServiceData>>()
    private val _managers = mutableMapOf<String, UARTManager>()

    internal fun registerManager(deviceId: String, manager: UARTManager) {
        _managers[deviceId] = manager
    }

    fun getData(deviceId: String): Flow<UARTServiceData> {
        return _dataMap.getOrPut(deviceId) { MutableStateFlow(UARTServiceData()) }
    }

    fun updateMaxWriteLength(deviceId: String, maxWriteLength: Int) {
        _dataMap[deviceId]?.update {
            it.copy(maxWriteLength = maxWriteLength)
        }
    }

    fun onNewMessageReceived(deviceId: String, message: String) {
        _dataMap[deviceId]?.update {
            it.copy(messages = it.messages + UARTRecord(message, UARTRecordType.OUTPUT))
        }
    }

    private fun getMaxWriteLength(deviceId: String): Int {
        return _dataMap[deviceId]?.value?.maxWriteLength ?: 20
    }

    fun onNewMessageSent(deviceId: String, message: String) {
        _dataMap[deviceId]?.update {
            it.copy(messages = it.messages + UARTRecord(message, UARTRecordType.INPUT))
        }
    }

    suspend fun sendText(deviceId: String, text: String, newLineChar: MacroEol) {
        _managers[deviceId]?.sendText(
            text.parseWithNewLineChar(newLineChar),
            getMaxWriteLength(deviceId),
        )
    }

    suspend fun runMacro(deviceId: String, macro: UARTMacro) {
        if (macro.command == null) return
        _managers[deviceId]?.sendText(
            macro.command!!.parseWithNewLineChar(macro.newLineChar),
            getMaxWriteLength(deviceId),
        )
    }

    fun clear(deviceId: String) {
        _dataMap.remove(deviceId)
        _managers.remove(deviceId)
    }

    fun clearOutputItems(deviceId: String) {
        _dataMap[deviceId]?.update {
            it.copy(messages = emptyList())
        }
    }

    fun deleteConfiguration(deviceId: String, configuration: UARTConfiguration) {
        _dataMap[deviceId]?.update {
            it.copy(uartViewState = it.uartViewState.copy(configurations = it.uartViewState.configurations - configuration))
        }
    }

    fun addConfiguration(address: String, configuration: UARTConfiguration) {
        // Add the new configuration to the list
        _dataMap[address]?.update {
            val newConfig = configuration.copy(id = it.uartViewState.configurations.size + 1)
            it.copy(uartViewState = it.uartViewState.copy(configurations = it.uartViewState.configurations + newConfig))
        }
    }

    fun updateSelectedConfigurationName(address: String, configurationName: String) {
        _dataMap[address]?.update {
            it.copy(uartViewState = it.uartViewState.copy(selectedConfigurationName = configurationName))
        }
    }

    fun loadPreviousConfigurations(address: String, configuration: List<UARTConfiguration>) {
        _dataMap[address]?.update {
            it.copy(uartViewState = it.uartViewState.copy(configurations = configuration))
        }
    }

    fun removeSelectedConfiguration(address: String) {
        _dataMap[address]?.update {
            it.copy(uartViewState = it.uartViewState.copy(selectedConfigurationName = null))
        }
    }

    fun onEditConfiguration(address: String) {
        _dataMap[address]?.update {
            it.copy(uartViewState = it.uartViewState.copy(isConfigurationEdited = !it.uartViewState.isConfigurationEdited))
        }
    }

    fun onEditMacro(address: String, editPosition: Int?) {
        _dataMap[address]?.update {
            it.copy(uartViewState = it.uartViewState.copy(editedPosition = editPosition))
        }
    }

    fun addOrEditMacro(address: String, macro: UARTMacro): UARTConfiguration? {
        var newConfig: UARTConfiguration? = null
        _dataMap[address]?.update {
            it.uartViewState.selectedConfiguration?.let { selectedConfiguration ->
                val macros = selectedConfiguration.macros.toMutableList().apply {
                    set(it.uartViewState.editedPosition!!, macro)
                }
                newConfig = selectedConfiguration.copy(macros = macros)
                // Save the new configuration and edited position.
                val newConfiguration = it.uartViewState.configurations.map { config ->
                    if (config.id == selectedConfiguration.id) {
                        newConfig
                    } else {
                        config
                    }
                }
                it.copy(
                    uartViewState = it.uartViewState.copy(
                        configurations = newConfiguration,
                        editedPosition = null
                    )
                )
            }!!
        }
        return newConfig
    }

    fun onEditFinished(address: String) {
        _dataMap[address]?.update {
            it.copy(uartViewState = it.uartViewState.copy(editedPosition = null))
        }
    }

    fun onDeleteMacro(address: String) {
        _dataMap[address]?.update {
            it.uartViewState.selectedConfiguration?.let { selectedConfiguration ->
                val macros = selectedConfiguration.macros.toMutableList().apply {
                    set(it.uartViewState.editedPosition!!, null)
                }
                val newConfig = selectedConfiguration.copy(macros = macros)

                // Save the new configuration and edited position.
                val newConfiguration = it.uartViewState.configurations.map { config ->
                    if (config.id == selectedConfiguration.id) {
                        newConfig
                    } else {
                        config
                    }
                }
                it.copy(
                    uartViewState = it.uartViewState.copy(
                        configurations = newConfiguration,
                        editedPosition = null
                    )
                )
            }!!
        }
    }
}