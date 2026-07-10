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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.analytics.AppAnalytics
import no.nordicsemi.android.analytics.Link
import no.nordicsemi.android.analytics.LinkOpenEvent
import no.nordicsemi.android.common.logger.LoggerLauncher
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.log.ILogSession
import no.nordicsemi.android.service.profile.ProfileServiceManager
import no.nordicsemi.android.service.profile.ServiceApi
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.argName
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
    private val analytics: AppAnalytics,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : SimpleNavigationViewModel(navigator, savedStateHandle) {
    val address: String
    val name: String?

    init {
        val arg = parameterOf(ProfileDestinationId)
        address = arg.getString(argAddress)!!
        name = arg.getString(argName)
    }

    private lateinit var peripheral: Peripheral
    private lateinit var serviceApi: ServiceApi
    private var logSession: ILogSession? = null

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            serviceApi = profileServiceManager.bindService()
            peripheral = serviceApi.getPeripheral(address)
            connectToPeripheral()
            observeConnectedDevices()
            observerDisconnection()
        }
    }

    private fun observeConnectedDevices() {
        val api = serviceApi

        api.devices
            .mapNotNull { devices -> devices[address] }
            .onEach { deviceData ->
                logSession = serviceApi.getLogSession(address)

                // The device may be in Connecting or Connected state.
                // When in Connecting, the ProfileUiState is Loading.
                if (deviceData.connectionState.isConnected) {
                    _uiState.value = ProfileUiState.Connected(deviceData)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observerDisconnection() {
        val api = serviceApi

        api.disconnectionEvent
            .filter { it.address == address }
            .onEach { event ->
                // If the device is not in the map, it's disconnected.
                // Check if there's a specific disconnection event for this device.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    channelSoundingManager.get()?.closeSession(address)
                }
                // Close channel sounding session if active
                _uiState.value = ProfileUiState.Disconnected(event.reason)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Connect to the peripheral with the given address.
     *
     * Before connecting, the service must be bound.
     * The service will be started if not already running.
     */
    private fun connectToPeripheral() {
        if (peripheral.isDisconnected) {
            profileServiceManager.connectToPeripheral(address, name)
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
                serviceApi.disconnect(address)
            }

            ConnectionEvent.NavigateUp -> {
                // Disconnect only if services are missing, otherwise leave connected
                if ((_uiState.value as? ProfileUiState.Connected)?.deviceData?.notSupported == true) {
                    serviceApi.disconnect(address)
                }
                navigator.navigateUp()
            }

            ConnectionEvent.OnRetryClicked -> {
                _uiState.value = ProfileUiState.Loading
                connectToPeripheral()
            }

            ConnectionEvent.OnForgetClicked -> {
                viewModelScope.launch {
                    serviceApi.forget(address)
                    connectToPeripheral()
                }
            }

            ConnectionEvent.OpenLoggerEvent -> openLogger()
        }
    }

    private fun openLogger() {
        analytics.logEvent(LinkOpenEvent(Link.LOGGER))
        LoggerLauncher.launch(context, logSession)
    }
}