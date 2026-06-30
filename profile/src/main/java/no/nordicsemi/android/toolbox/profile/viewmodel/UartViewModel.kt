package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.analytics.AppAnalytics
import no.nordicsemi.android.analytics.UARTChangeConfiguration
import no.nordicsemi.android.analytics.UARTCreateConfiguration
import no.nordicsemi.android.analytics.UARTMode
import no.nordicsemi.android.analytics.UARTSendAnalyticsEvent
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.data.UARTServiceData
import no.nordicsemi.android.toolbox.profile.data.uart.MacroEol
import no.nordicsemi.android.toolbox.profile.data.uart.UARTConfiguration
import no.nordicsemi.android.toolbox.profile.data.uart.UARTMacro
import no.nordicsemi.android.toolbox.profile.manager.repository.UartRepository
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import no.nordicsemi.android.toolbox.profile.repository.uartXml.UartConfigurationRepository

// UART Profile events.
internal sealed interface UARTEvent {
    data class OnCreateMacro(
        val macroName: UARTMacro,
    ) : UARTEvent

    data class OnEditMacro(
        val position: Int,
    ) : UARTEvent

    data object OnEditFinished : UARTEvent
    data object OnDeleteMacro : UARTEvent

    data class OnRunMacro(
        val macro: UARTMacro,
    ) : UARTEvent

    data class OnConfigurationSelected(
        val configuration: UARTConfiguration,
    ) : UARTEvent

    data class OnAddConfiguration(
        val name: String,
    ) : UARTEvent

    data object OnEditConfiguration : UARTEvent
    data class OnDeleteConfiguration(
        val configuration: UARTConfiguration,
    ) : UARTEvent

    data class OnRunInput(
        val text: String,
        val newLineChar: MacroEol,
    ) : UARTEvent

    data object ClearOutputItems : UARTEvent

    data class SetMaxValueLength(
        val maxValueLength: Int,
    ) : UARTEvent

}

@HiltViewModel
internal class UartViewModel @Inject constructor(
    private val uartConfigurationRepository: UartConfigurationRepository,
    private val analytics: AppAnalytics,
    private val deviceRepository: DeviceRepository,
    navigator: Navigator,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    private val address = parameterOf(ProfileDestinationId).getString(argAddress)!!

    private val _state = MutableStateFlow(UARTServiceData())
    val state = _state.asStateFlow()

    init {
        observeUartProfile()
    }

    /**
     * Observes the UART profile from the device repository.
     */
    private fun observeUartProfile() {
        viewModelScope.launch {
            // Observe the profile handler flow from the device repository.
            deviceRepository.profileHandlerFlow
                .filter { it.isNotEmpty() }
                .onEach { mapOfPeripheralProfiles ->
                    mapOfPeripheralProfiles.forEach { (peripheral, profiles) ->
                        if (peripheral.address == address) {
                            profiles.filter { it.profile == Profile.UART }
                                .forEach { _ ->
                                    startUartService()
                                }
                        }
                    }
                }
                .launchIn(this)
        }
    }

    /**
     * Starts the UART service and observes the data.
     */
    private fun startUartService() {
        // Initialize the UART repository with the address.
        UartRepository.getData(address)
            .onEach { data ->
                _state.value = data
            }
            .launchIn(viewModelScope)
        // Observe the UART configurations.
        observeConfigurations()
    }

    /**
     * Observes the UART configurations from the repository.
     * It updates the selected configuration name and loads previous configurations.
     */
    private fun observeConfigurations(): Unit = with(uartConfigurationRepository) {
        // Get the last configuration name from the data store.
        getLastConfigurationName()
            .filterNotNull()
            .onEach { name ->
                UartRepository.updateSelectedConfigurationName(address, name)
            }
            .launchIn(viewModelScope)

        // Get all configurations for the device.
        getAllConfigurations()
            .onEach { uartConfigurations ->
                UartRepository.loadPreviousConfigurations(address, uartConfigurations)
            }
            .launchIn(viewModelScope)
    }

    // UART events.
    fun onEvent(event: UARTEvent) {
        when (event) {
            UARTEvent.ClearOutputItems -> UartRepository.clearOutputItems(address) // working.
            is UARTEvent.OnAddConfiguration -> onAddConfiguration(event.name) // TODO: check if it is working.

            is UARTEvent.OnConfigurationSelected -> onConfigurationSelected(event.configuration)
            is UARTEvent.OnCreateMacro -> addNewMacro(event.macroName)
            is UARTEvent.OnDeleteConfiguration -> deleteConfiguration(event.configuration)
            UARTEvent.OnDeleteMacro -> onDeleteMacro()
            is UARTEvent.OnEditConfiguration -> onEditConfiguration()
            UARTEvent.OnEditFinished -> onEditFinished()
            is UARTEvent.OnEditMacro -> onEditMacro(event.position)
            is UARTEvent.OnRunInput -> {
                sendText(event.text, event.newLineChar)
            }

            is UARTEvent.OnRunMacro -> runMacro(event.macro)
            is UARTEvent.SetMaxValueLength ->
                UartRepository.updateMaxWriteLength(address, event.maxValueLength)
        }
    }

    /**
     * Deletes the macro from the repository.
     */
    private fun onDeleteMacro() {
        viewModelScope.launch(Dispatchers.IO) {
            UartRepository.onDeleteMacro(address)
        }
    }

    /**
     * Called when the edit is finished.
     * It notifies the repository that the edit is finished.
     */
    private fun onEditFinished() {
        viewModelScope.launch {
            UartRepository.onEditFinished(address)
        }
    }

    /**
     * Adds a new macro to the repository.
     * It saves the new macro to the database.
     */
    private fun addNewMacro(macroName: UARTMacro) {
        viewModelScope.launch(Dispatchers.IO) {
            val newConfig = UartRepository.addOrEditMacro(address, macroName)
            if (newConfig != null) {
                // Save the new configuration to the database.
                uartConfigurationRepository.insertConfiguration(newConfig)
            }
        }
    }

    /**
     * Called when a macro is edited.
     * It notifies the repository that the macro is edited.
     */
    private fun onEditMacro(position: Int) {
        viewModelScope.launch {
            // Update the configuration in the UART repository.
            UartRepository.onEditMacro(address, position)
        }
    }

    /**
     * Edit uart configuration.
     */
    private fun onEditConfiguration() {
        viewModelScope.launch {
            // Update the configuration in the UART repository.
            UartRepository.onEditConfiguration(address)
        }
    }

    /**
     * Runs the macro.
     */
    private fun runMacro(macro: UARTMacro) {
        viewModelScope.launch {
            UartRepository.runMacro(address, macro)
            // Log the event in the analytics.
            analytics.logEvent(UARTSendAnalyticsEvent(UARTMode.PRESET))
        }
    }

    /**
     * Adds a new configuration to the repository and database.
     */
    private fun onAddConfiguration(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update the configuration in the UART repository.
            UartRepository.updateSelectedConfigurationName(address, name)
            // Add configuration to the database.
            val configurationId =
                uartConfigurationRepository.insertConfiguration(UARTConfiguration(null, name))
                    ?: return@launch
            // Add configuration to the repository.
            UartRepository.addConfiguration(address, UARTConfiguration(configurationId.toInt(), name))

            // Save the configuration name in the data store.
            uartConfigurationRepository.saveLastConfigurationNameToDataSource(name)
            // Log the event in the analytics.
            analytics.logEvent(UARTCreateConfiguration())
        }
    }

    /**
     * Called when a configuration is selected.
     * It updates the selected configuration in the repository and saves it to the data store.
     */
    private fun onConfigurationSelected(configuration: UARTConfiguration) {
        viewModelScope.launch {
            UartRepository.updateSelectedConfigurationName(address, configuration.name)
            // Update the selected configuration in the datastore.
            uartConfigurationRepository.saveLastConfigurationNameToDataSource(configuration.name)
            // Log the event in the analytics.
            analytics.logEvent(UARTChangeConfiguration())
        }
    }

    /**
     * Deletes the configuration from the repository and database.
     * It also removes the selected configuration if it is deleted.
     */
    private fun deleteConfiguration(configuration: UARTConfiguration) {
        viewModelScope.launch(Dispatchers.IO) {
            // delete the configuration from the list.
            UartRepository.deleteConfiguration(address, configuration)
            // remove the selected configuration if it is deleted.
            UartRepository.removeSelectedConfiguration(address)
            // delete the configuration from the database.
            uartConfigurationRepository.deleteConfiguration(configuration)
        }
    }

    /**
     * Sends the text to the UART device.
     */
    private fun sendText(text: String, newLineChar: MacroEol) {
        viewModelScope.launch {
            UartRepository.sendText(address, text, newLineChar)
            // Log the event in the analytics.
            analytics.logEvent(UARTSendAnalyticsEvent(UARTMode.TEXT))
        }
    }
}
