package no.nordicsemi.android.toolbox.profile.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.analytics.AppAnalytics
import no.nordicsemi.android.analytics.Link
import no.nordicsemi.android.analytics.ProfileOpenEvent
import no.nordicsemi.android.common.logger.LoggerLauncher
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.log.LogSession
import no.nordicsemi.android.log.timber.nRFLoggerTree
import no.nordicsemi.android.service.profile.ProfileServiceManager
import no.nordicsemi.android.service.profile.ServiceApi
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import no.nordicsemi.android.toolbox.profile.repository.channelSounding.ChannelSoundingManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val profileServiceManager: ProfileServiceManager,
    private val channelSoundingManager: Provider<ChannelSoundingManager>,
    private val navigator: Navigator,
    private val deviceRepository: DeviceRepository,
    private val analytics: AppAnalytics,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    val address: String = parameterOf(ProfileDestinationId)
    var peripheral: Peripheral? = null
    private var serviceApi: ServiceApi? = null
    private val logger: nRFLoggerTree = nRFLoggerTree(context, address, context.getString(R.string.app_name))
        .apply { setLoggingTagsEnabled(false) }

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        connectToPeripheral()
        observeConnectedDevices()
        Timber.plant(logger)
    }

    private suspend fun getServiceApi() =
        profileServiceManager.bindService().also {
            serviceApi = it
            peripheral = it.getPeripheral(address)
        }

    private fun observeConnectedDevices() = viewModelScope.launch {
        // Combine flows from the service to create a single UI state.
        val api = getServiceApi()

        // Combine flows from the service to create a single UI state.
        combine(
            api.devices,
            api.isMissingServices,
            api.disconnectionEvent
        ) { devices, missingServicesMap, disconnection ->
            deviceRepository.updateConnectedDevices(devices)
            val deviceData = devices[address]
            val isMissingServices = missingServicesMap[address] ?: false
            // Determine the UI state based on the service's state
            if (deviceData != null) {
                // Update connected device info in the repository
                deviceRepository.updateProfilePeripheralPair(
                    deviceData.peripheral,
                    deviceData.services
                )
                deviceData.services.forEach {
                    deviceRepository.updateAnalytics(
                        address,
                        it.profile
                    )
                }
                val currentMaxVal =
                    (_uiState.value as? ProfileUiState.Connected)?.maxValueLength
                ProfileUiState.Connected(deviceData, isMissingServices, currentMaxVal)
            } else {
                // If the device is not in the map, it's disconnected.
                // Check if there's a specific disconnection event for this device.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    channelSoundingManager.get()?.closeSession(address)
                }
                val reason =
                    if (disconnection?.address == address) disconnection.reason else null
                deviceRepository.removeLoggedProfile(address)
                // Close channel sounding session if active
                ProfileUiState.Disconnected(reason)
            }
        }.catch { e ->
            Timber.e(e, "Error observing profile state")
            // You could emit a generic error state here if needed
        }.collect { state ->
            _uiState.value = state
        }
    }

    /**
     * Connect to the peripheral with the given address. Before connecting, the service must be bound.
     * The service will be started if not already running.
     */
    private fun connectToPeripheral() = viewModelScope.launch {
        // Connect to the peripheral
        getServiceApi().let {
            if (it.getPeripheral(address) == null) peripheral = it.getPeripheral(address)
            if (peripheral?.isConnected != true) {
                profileServiceManager.connectToPeripheral(address)
            }
        }
    }


    fun onEvent(event: ConnectionEvent) {
        when (event) {
            ConnectionEvent.DisconnectEvent -> {
                // if the profile is channel sounding then we need to stop the ranging session before disconnecting.
                if (_uiState.value is ProfileUiState.Connected) {
                    val state = _uiState.value as ProfileUiState.Connected
                    if (state.deviceData.services.any { it.profile == Profile.CHANNEL_SOUNDING }) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                            try {
                                channelSoundingManager.get().closeSession(address)
                            } catch (e: Exception) {
                                Timber.e(" ${e.message}")
                            }
                        }
                    }
                }
                serviceApi?.disconnect(address)
            }

            ConnectionEvent.NavigateUp -> {
                // Disconnect only if services are missing, otherwise leave connected
                if ((_uiState.value as? ProfileUiState.Connected)?.isMissingServices == true) {
                    serviceApi?.disconnect(address)
                }
                navigator.navigateUp()
            }

            ConnectionEvent.OnRetryClicked -> {
                _uiState.value = ProfileUiState.Loading
                connectToPeripheral()
            }

            ConnectionEvent.OpenLoggerEvent -> openLogger()
            ConnectionEvent.RequestMaxValueLength -> requestMaxWriteValue()
        }
    }

    private fun requestMaxWriteValue() = viewModelScope.launch {
        val mtu = serviceApi?.getMaxWriteValue(address)
        _uiState.update {
            (it as? ProfileUiState.Connected)?.copy(maxValueLength = mtu) ?: it
        }
    }

    private fun openLogger() {
        analytics.logEvent(ProfileOpenEvent(Link.LOGGER))
        LoggerLauncher.launch(context, logger.session as? LogSession)
    }

    override fun onCleared() {
        Timber.uproot(logger)
        profileServiceManager.unbindService()
        serviceApi = null
        super.onCleared()
    }
}