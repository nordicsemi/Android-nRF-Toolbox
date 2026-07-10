package no.nordicsemi.android.service.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.timber.nRFLoggerTree
import no.nordicsemi.android.analytics.AppAnalytics
import no.nordicsemi.android.analytics.ProfileConnectedEvent
import no.nordicsemi.android.service.NotificationService
import no.nordicsemi.android.service.R
import no.nordicsemi.android.toolbox.profile.manager.ServiceManager
import no.nordicsemi.android.toolbox.profile.manager.ServiceManagerFactory
import no.nordicsemi.kotlin.ble.client.RemoteServices
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.CentralManager.ConnectionOptions
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.BondState
import no.nordicsemi.kotlin.ble.core.ConnectionState
import no.nordicsemi.kotlin.ble.core.WriteType
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ProfileService : NotificationService() {

    @Inject
    lateinit var centralManager: CentralManager
    @Inject
    lateinit var analytics: AppAnalytics

    private val binder = LocalBinder()
    private val managedConnections = mutableMapOf<String, Job>()

    private val _devices = MutableStateFlow<Map<String, ServiceApi.DeviceData>>(emptyMap())
    private val _isMissingServices = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _disconnectionEvent = MutableStateFlow<ServiceApi.DisconnectionEvent?>(null)

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.getStringExtra(DEVICE_ADDRESS)?.let { address ->
            connect(address)
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        managedConnections.values.forEach { it.cancel() }
        uprootLogger()
        super.onDestroy()
    }

    /**
     * Initiates a connection to the peripheral with the given address.
     */
    private fun connect(address: String) {
        if (managedConnections.containsKey(address)) return

        initLogger(address)

        val peripheral = centralManager.getPeripheralById(address) ?: run {
            Timber.w("Peripheral with address $address not found.")
            return
        }

        val job = lifecycleScope.launch {
            // Called when a profile's initialize() completes — add manager to _devices.services.
            val onReady: (ServiceManager) -> Unit = { manager ->
                _devices.update { currentMap ->
                    val existingData = currentMap[address] ?: return@update currentMap
                    currentMap + (address to existingData.copy(
                        services = (existingData.services + manager).sortedBy { it.profile.ordinal }
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
                                    services = emptyList()
                                ))
                            }
                        }
                        is RemoteServices.Discovered -> {
                            val list = services.services
                            if (list.any { ServiceManagerFactory.isKnownService(it.uuid) }) {
                                _isMissingServices.update { it - address }
                            } else {
                                _isMissingServices.update { it + (address to true) }
                            }
                        }
                        else -> {}
                    }
                }
                .launchIn(this)

            try {
                centralManager.connect(peripheral, options = ConnectionOptions.Direct())
            } catch (e: Exception) {
                Timber.e(e, "Failed to connect to $address")
            }

            observeConnectionState(peripheral)
        }

        managedConnections[address] = job
    }

    /**
     * Observes the connection state of the given peripheral and updates the service state accordingly.
     */
    private fun CoroutineScope.observeConnectionState(peripheral: Peripheral) {
        peripheral.state
            .onEach { state ->
                _devices.update {
                    it + (peripheral.address to (it[peripheral.address]?.copy(connectionState = state)
                        ?: ServiceApi.DeviceData(peripheral, state)))
                }

                when (state) {
                    is ConnectionState.Disconnected -> {
                        val reason = state.reason ?: ConnectionState.Disconnected.Reason.Success
                        _disconnectionEvent.value =
                            ServiceApi.DisconnectionEvent(peripheral.address, reason)
                        _devices.update { it - peripheral.address }
                        handleDisconnection(peripheral.address)
                    }

                    else -> {}
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
                    Timber.e(e, "Failed to disconnect from $address")
                    handleDisconnection(address)
                }
            }
        }
    }

    private fun handleDisconnection(address: String) {
        _devices.update { it - address }
        _isMissingServices.update { it - address }
        managedConnections[address]?.cancel()
        managedConnections.remove(address)
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
    private fun initLogger(deviceAddress: String) {
        if (logger != null) return
        logger = nRFLoggerTree(this, getString(R.string.app_name), deviceAddress)
            .also { Timber.plant(it) }
    }

    /**
     * Uproots and clears the logger.
     */
    private fun uprootLogger() {
        logger?.let { Timber.uproot(it) }
        logger = null
    }

    // The Binder providing the public API.
    inner class LocalBinder : Binder(), ServiceApi {
        override val devices: StateFlow<Map<String, ServiceApi.DeviceData>>
            get() = _devices.asStateFlow()

        override val isMissingServices: StateFlow<Map<String, Boolean>>
            get() = _isMissingServices.asStateFlow()

        override val disconnectionEvent: StateFlow<ServiceApi.DisconnectionEvent?>
            get() = _disconnectionEvent.asStateFlow()

        override fun disconnect(address: String) = this@ProfileService.disconnect(address)

        override fun getPeripheral(address: String?): Peripheral? =
            address?.let { centralManager.getPeripheralById(it) }

        override suspend fun getMaxWriteValue(address: String, writeType: WriteType): Int? {
            val peripheral = getPeripheral(address) ?: return null
            if (!peripheral.isConnected) return null

            return try {
                peripheral.requestHighestValueLength()
                peripheral.maximumWriteValueLength(writeType)
            } catch (e: Exception) {
                Timber.e(e, "Failed to configure MTU for $address")
                null
            }
        }

        override suspend fun createBond(address: String) {
            getPeripheral(address)?.ensureBonded()
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
