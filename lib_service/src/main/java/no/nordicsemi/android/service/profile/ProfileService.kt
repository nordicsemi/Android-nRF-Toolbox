package no.nordicsemi.android.service.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.timber.nRFLoggerTree
import no.nordicsemi.android.analytics.AppAnalytics
import no.nordicsemi.android.analytics.ProfileConnectedEvent
import no.nordicsemi.android.service.NotificationService
import no.nordicsemi.android.toolbox.profile.manager.ServiceManager
import no.nordicsemi.android.toolbox.profile.manager.ServiceManagerFactory
import no.nordicsemi.kotlin.ble.client.RemoteServices
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.CentralManager.ConnectionOptions
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.BondState
import no.nordicsemi.kotlin.ble.core.ConnectionState
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ProfileService : NotificationService() {
    companion object {
        const val DEVICE_ADDRESS = "deviceAddress"
        const val DEVICE_NAME = "deviceName"
    }

    @Inject
    lateinit var centralManager: CentralManager
    @Inject
    lateinit var analytics: AppAnalytics

    private val binder = LocalBinder()
    private val managedConnections = mutableMapOf<String, Job>()
    private val loggers = mutableMapOf<String, nRFLoggerTree>()

    private val _devices = MutableStateFlow<Map<String, ServiceApi.DeviceData>>(emptyMap())
    private val _disconnectionEvent = MutableSharedFlow<ServiceApi.DisconnectionEvent>()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val address = intent?.getStringExtra(DEVICE_ADDRESS)
        val name = intent?.getStringExtra(DEVICE_NAME)
        address?.let { address ->
            initLogger(address, name)
            connect(address)
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        managedConnections.values.forEach { it.cancel() }
        loggers.values.forEach {
            try {
                Timber.uproot(it)
            } catch (_: IllegalArgumentException) {
                // Already uprooted, ignore.
            }
        }
        super.onDestroy()
    }

    /**
     * Initiates a connection to the peripheral with the given address.
     */
    private fun connect(address: String) {
        if (managedConnections.containsKey(address)) return

        val peripheral = centralManager.getPeripheralById(address) ?: run {
            Timber.tag(address).w("Peripheral with address $address not found.")
            return
        }

        val job = lifecycleScope.launch {
            // Called when a profile's initialize() completes — add manager to _devices.services.
            val onReady: (ServiceManager) -> Unit = { manager ->
                _devices.update { currentMap ->
                    val existingData = currentMap[address] ?: return@update currentMap
                    currentMap + (address to existingData.copy(
                        services = (existingData.services + manager).sortedBy { it.profile.ordinal },
                        notSupported = false,
                    ))
                }
                analytics.logEvent(ProfileConnectedEvent(manager.profile))
            }

            // Register all known profiles before connecting. Each activates when its service
            // is discovered; prepare() validates characteristics, initialize() sets up streams.
            ServiceManagerFactory.createAllManagers(this@ProfileService, address, onReady)
                .forEach { peripheral.profile(this, it, required = false) }

            // Track whether any known service was found after discovery.
            peripheral.services()
                .onEach { services ->
                    when (services) {
                        is RemoteServices.Unknown -> {
                            _devices.update { currentMap ->
                                val existingData = currentMap[address] ?: return@update currentMap
                                currentMap + (address to existingData.copy(
                                    services = emptyList(),
                                    notSupported = null,
                                ))
                            }
                        }
                        is RemoteServices.Discovered -> {
                            val list = services.services
                            if (list.none { ServiceManagerFactory.isKnownService(it.uuid) }) {
                                _devices.update { currentMap ->
                                    val existingData = currentMap[address] ?: return@update currentMap
                                    currentMap + (address to existingData.copy(notSupported = true))
                                }
                            }
                        }
                        else -> {}
                    }
                }
                .launchIn(this)

            observeConnectionState(peripheral)

            try {
                centralManager.connect(peripheral, options = ConnectionOptions.Direct(
                    automaticallyRequestHighestValueLength = true,
                ))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(address).e(e, "Failed to connect to $address")
            }
        }

        managedConnections[address] = job
    }

    /**
     * Observes the connection state of the given peripheral and updates the service state accordingly.
     */
    private fun CoroutineScope.observeConnectionState(peripheral: Peripheral) {
        peripheral.state
            .onEach { state ->
                when (state) {
                    is ConnectionState.Disconnecting -> {
                        _disconnectionEvent.emit(ServiceApi.DisconnectionEvent(peripheral.address))
                        handleDisconnection(peripheral.address)
                    }

                    is ConnectionState.Disconnected -> {
                        val reason = state.reason ?: ConnectionState.Disconnected.Reason.Success
                        _disconnectionEvent.emit(ServiceApi.DisconnectionEvent(peripheral.address, reason))
                        handleDisconnection(peripheral.address)
                    }

                    else -> {
                        _devices.update {
                            it + (peripheral.address to (it[peripheral.address]?.copy(connectionState = state)
                                ?: ServiceApi.DeviceData(peripheral, state)))
                        }
                    }
                }
            }
            .launchIn(this)
    }

    /**
     * Disconnects from the peripheral with the given address.
     */
    @SuppressLint("TimberExceptionLogging")
    private fun disconnect(address: String) {
        centralManager.getPeripheralById(address)?.let { peripheral ->
            lifecycleScope.launch {
                try {
                    peripheral.disconnect()
                } catch (e: Exception) {
                    Timber.tag(address).e(e, "Failed to disconnect from $address")
                }
                handleDisconnection(address)
            }
        }
    }

    private fun handleDisconnection(address: String) {
        _devices.update { it - address }
        managedConnections[address]?.cancel()
        managedConnections.remove(address)
        uprootLogger(address)
        stopServiceIfNoDevices()
    }

    private fun stopServiceIfNoDevices() {
        if (_devices.value.isEmpty()) {
            stopForegroundService()
            stopSelf()
        }
    }

    /**
     * Initializes the logger if not already initialized.
     */
    private fun initLogger(deviceAddress: String, deviceName: String?) {
        if (loggers.contains(deviceAddress)) return

        val logger = object : nRFLoggerTree(this, deviceAddress, deviceName) {
            init {
                // Disable logging tags in nRF Logger.
                setLoggingTagsEnabled(false)
            }

            override fun isLoggable(tag: String?, priority: Int): Boolean {
                // Log general events or sent by the device with given address.
                val hasSession = super.isLoggable(tag, priority)
                val genericTag = tag == null || !tag.contains(":")
                val thisDevice = tag?.contains(deviceAddress) ?: false
                return hasSession && (genericTag || thisDevice)
            }
        }
        loggers[deviceAddress] = logger.also { Timber.plant(it) }
    }

    /**
     * Uproots and clears the logger for the given device.
     */
    private fun uprootLogger(deviceAddress: String) {
        loggers[deviceAddress]?.let {
            try {
                Timber.uproot(it)
            } catch (_: IllegalArgumentException) {
                // Already uprooted, ignore.
            }
        }
        loggers.remove(deviceAddress)
    }

    // The Binder providing the public API.
    inner class LocalBinder : Binder(), ServiceApi {
        override val devices: StateFlow<Map<String, ServiceApi.DeviceData>>
            get() = _devices.asStateFlow()

        override val disconnectionEvent: SharedFlow<ServiceApi.DisconnectionEvent>
            get() = _disconnectionEvent.asSharedFlow()

        override fun disconnect(address: String) = this@ProfileService.disconnect(address)

        override fun getPeripheral(address: String) = centralManager.getPeripheralById(address)!!

        override fun getLogSession(address: String) = loggers[address]?.session

        override suspend fun createBond(address: String) {
            getPeripheral(address).ensureBonded()
        }

        override suspend fun forget(address: String) {
            try {
                // This method may throw: OperationFailedException(reason=Request Failed)
                getPeripheral(address).removeBond()
            } catch (e: Exception) {
                Timber.tag(address).e("Failed to remove bond information: ${e.message}")
            }
        }
    }

    /**
     * Ensures the peripheral is bonded. If not, initiates bonding.
     */
    private suspend fun Peripheral.ensureBonded() {
        if (this.bondState.value == BondState.BONDED) return
        createBond()
    }
}
