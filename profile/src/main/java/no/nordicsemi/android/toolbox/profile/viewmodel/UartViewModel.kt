package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.analytics.AppAnalytics
import no.nordicsemi.android.analytics.UARTChangeConfiguration
import no.nordicsemi.android.analytics.UARTCreateConfiguration
import no.nordicsemi.android.analytics.UARTMode
import no.nordicsemi.android.analytics.UARTSendAnalyticsEvent
import no.nordicsemi.android.toolbox.profile.data.uart.MacroEol
import no.nordicsemi.android.toolbox.profile.data.uart.UARTConfiguration
import no.nordicsemi.android.toolbox.profile.data.uart.UARTMacro
import no.nordicsemi.android.toolbox.profile.data.uart.parseWithNewLineChar
import no.nordicsemi.android.toolbox.profile.manager.UARTManager
import no.nordicsemi.android.toolbox.profile.repository.uartXml.UartConfigurationRepository

internal sealed interface UARTEvent {
    data class OnCreateMacro(val macroName: UARTMacro) : UARTEvent
    data class OnEditMacro(val position: Int) : UARTEvent
    data object OnEditFinished : UARTEvent
    data object OnDeleteMacro : UARTEvent
    data class OnRunMacro(val macro: UARTMacro) : UARTEvent
    data class OnConfigurationSelected(val configuration: UARTConfiguration) : UARTEvent
    data class OnAddConfiguration(val name: String) : UARTEvent
    data object OnEditConfiguration : UARTEvent
    data class OnDeleteConfiguration(val configuration: UARTConfiguration) : UARTEvent
    data class OnRunInput(val text: String, val newLineChar: MacroEol) : UARTEvent
    data object ClearOutputItems : UARTEvent
    data class SetMaxValueLength(val maxValueLength: Int) : UARTEvent
}

@HiltViewModel(assistedFactory = UartViewModel.Factory::class)
internal class UartViewModel @AssistedInject constructor(
    @Assisted private val manager: UARTManager,
    private val uartConfigurationRepository: UartConfigurationRepository,
    private val analytics: AppAnalytics,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: UARTManager): UartViewModel }

    val state = manager.repository.data

    init {
        observeConfigurations()
    }

    private fun observeConfigurations(): Unit = with(uartConfigurationRepository) {
        getLastConfigurationName()
            .filterNotNull()
            .onEach { name ->
                manager.repository.updateSelectedConfigurationName(name)
            }
            .launchIn(viewModelScope)

        getAllConfigurations()
            .onEach { uartConfigurations ->
                manager.repository.loadPreviousConfigurations(uartConfigurations)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: UARTEvent) {
        when (event) {
            UARTEvent.ClearOutputItems ->
                manager.repository.clearOutputItems()

            is UARTEvent.OnAddConfiguration -> onAddConfiguration(event.name)
            is UARTEvent.OnConfigurationSelected -> onConfigurationSelected(event.configuration)
            is UARTEvent.OnCreateMacro -> addNewMacro(event.macroName)
            is UARTEvent.OnDeleteConfiguration -> deleteConfiguration(event.configuration)
            UARTEvent.OnDeleteMacro -> onDeleteMacro()
            is UARTEvent.OnEditConfiguration -> onEditConfiguration()
            UARTEvent.OnEditFinished -> onEditFinished()
            is UARTEvent.OnEditMacro -> onEditMacro(event.position)
            is UARTEvent.OnRunInput -> sendText(event.text, event.newLineChar)
            is UARTEvent.OnRunMacro -> runMacro(event.macro)
            is UARTEvent.SetMaxValueLength ->
                manager.repository.updateMaxWriteLength(event.maxValueLength)
        }
    }

    private fun onDeleteMacro() {
        viewModelScope.launch(Dispatchers.IO) {
            manager.repository.onDeleteMacro()
        }
    }

    private fun onEditFinished() {
        viewModelScope.launch {
            manager.repository.onEditFinished()
        }
    }

    private fun addNewMacro(macroName: UARTMacro) {
        viewModelScope.launch(Dispatchers.IO) {
            val newConfig = manager.repository.addOrEditMacro(macroName)
            if (newConfig != null) {
                uartConfigurationRepository.insertConfiguration(newConfig)
            }
        }
    }

    private fun onEditMacro(position: Int) {
        viewModelScope.launch {
            manager.repository.onEditMacro(position)
        }
    }

    private fun onEditConfiguration() {
        viewModelScope.launch {
            manager.repository.onEditConfiguration()
        }
    }

    private fun runMacro(macro: UARTMacro) {
        viewModelScope.launch {
            macro.command?.let { cmd ->
                manager.sendText(cmd.parseWithNewLineChar(macro.newLineChar), manager.repository.maxWriteLength)
            }
            analytics.logEvent(UARTSendAnalyticsEvent(UARTMode.PRESET))
        }
    }

    private fun onAddConfiguration(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            manager.repository.updateSelectedConfigurationName(name)
            val configurationId =
                uartConfigurationRepository.insertConfiguration(UARTConfiguration(null, name))
                    ?: return@launch
            manager.repository.addConfiguration(UARTConfiguration(configurationId.toInt(), name))
            uartConfigurationRepository.saveLastConfigurationNameToDataSource(name)
            analytics.logEvent(UARTCreateConfiguration())
        }
    }

    private fun onConfigurationSelected(configuration: UARTConfiguration) {
        viewModelScope.launch {
            manager.repository.updateSelectedConfigurationName(configuration.name)
            uartConfigurationRepository.saveLastConfigurationNameToDataSource(configuration.name)
            analytics.logEvent(UARTChangeConfiguration())
        }
    }

    private fun deleteConfiguration(configuration: UARTConfiguration) {
        viewModelScope.launch(Dispatchers.IO) {
            manager.repository.deleteConfiguration(configuration)
            manager.repository.removeSelectedConfiguration()
            uartConfigurationRepository.deleteConfiguration(configuration)
        }
    }

    private fun sendText(text: String, newLineChar: MacroEol) {
        viewModelScope.launch {
            manager.sendText(text.parseWithNewLineChar(newLineChar), manager.repository.maxWriteLength)
            analytics.logEvent(UARTSendAnalyticsEvent(UARTMode.TEXT))
        }
    }
}
