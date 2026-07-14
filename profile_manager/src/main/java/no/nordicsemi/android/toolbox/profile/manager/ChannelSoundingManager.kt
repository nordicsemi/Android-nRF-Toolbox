package no.nordicsemi.android.toolbox.profile.manager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.ranging.RangingData
import android.ranging.RangingDevice
import android.ranging.RangingManager
import android.ranging.RangingPreference
import android.ranging.RangingPreference.DEVICE_ROLE_INITIATOR
import android.ranging.RangingSession
import android.ranging.SensorFusionParams
import android.ranging.SessionConfig
import android.ranging.ble.cs.BleCsRangingCapabilities
import android.ranging.ble.cs.BleCsRangingParams
import android.ranging.raw.RawInitiatorRangingConfig
import android.ranging.raw.RawRangingDevice
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.RANGING_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.data.CSRangingMeasurement
import no.nordicsemi.android.toolbox.profile.data.ConfidenceLevel
import no.nordicsemi.android.toolbox.profile.data.CsRangingData
import no.nordicsemi.android.toolbox.profile.data.RangingSessionAction
import no.nordicsemi.android.toolbox.profile.data.RangingSessionFailedReason
import no.nordicsemi.android.toolbox.profile.data.RangingTechnology
import no.nordicsemi.android.toolbox.profile.data.SessionClosedReason
import no.nordicsemi.android.toolbox.profile.data.UpdateRate
import no.nordicsemi.android.toolbox.profile.manager.repository.ChannelSoundingRepository
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.BondState
import timber.log.Timber
import java.util.UUID
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val RAS_FEATURES = Uuid.parse("00002C14-0000-1000-8000-00805F9B34FB")

/**
 * Manages the Ranging Service (RAS) and the Channel Sounding ranging session for a single
 * connected device. The manager owns the [RangingSession] for [deviceId] end-to-end: it starts
 * ranging once the peripheral is bonded, exposes progress/results through [repository], and lets
 * the UI request a rate change or a manual restart.
 *
 * All session-lifecycle transitions are driven by [RangingSession.Callback] - [closeSession] only
 * requests a stop/close and returns immediately; the resulting state update (and any deferred
 * restart) happens once the system actually reports the session as stopped/closed.
 */
class ChannelSoundingManager(
    private val context: Context,
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(RANGING_SERVICE_UUID, deviceId, "Channel Sounding", onReady) {
    override val profile: ServiceType = ServiceType.CHANNEL_SOUNDING
    private val tag = "CS ($deviceId)"

    val repository = ChannelSoundingRepository()

    private lateinit var peripheral: Peripheral
    private var rasFeaturesCharacteristic: RemoteCharacteristic? = null

    private val rangingManager: RangingManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            context.getSystemService(RangingManager::class.java)
        } else {
            null
        }
    }

    private var activeSession: RangingSession? = null
    private var capabilitiesCallback: RangingManager.RangingCapabilitiesCallback? = null
    private val previousRangingData = mutableListOf<Float>()

    /** Rate the currently active (or last started) session was configured with. */
    private var currentRate: UpdateRate = UpdateRate.NORMAL

    /** True while the session is open, i.e. between `onOpened` and `onStopped`/`onClosed`. */
    private var isSessionOpen = false

    /** True once [closeSession] requested a stop/close, until `RangingSession.Callback.onClosed` fires. */
    private var closing = false

    /** Action to run once the pending close (requested via [closeSession]) completes. */
    private var pendingRestart: (() -> Unit)? = null

    override fun prepare(service: RemoteService) {
        peripheral = requireNotNull(service.owner as? Peripheral)
        rasFeaturesCharacteristic = service.characteristics.firstOrNull { it.uuid == RAS_FEATURES }
    }

    override suspend fun CoroutineScope.initialize() {
        rasFeaturesCharacteristic?.let { char ->
            launch {
                try {
                    Timber.tag(tag).v("Reading RAS features...")
                    val rasFeature = RasFeatureParser.parse(char.read())
                    Timber.tag(tag).log(Log.Level.APPLICATION, "Features: $rasFeature")
                } catch (e: Exception) {
                    Timber.tag(tag).e(e, "Error reading RAS features")
                }
            }
        }

        onReady(this@ChannelSoundingManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            launch {
                // Channel Sounding requires the devices to be bonded before ranging can start.
                peripheral.bondState.first { it == BondState.BONDED }
                startRangingMeasurement()
            }
        }
    }

    /**
     * Changes the ranging update rate. The UI reflects the new rate immediately; if a session is
     * currently running with a different rate, it is closed and restarted with the new rate once
     * the close completes.
     */
    fun changeUpdateRate(rate: UpdateRate) {
        repository.updateRate(rate)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA || rate == currentRate) return
        closeSession { startRangingMeasurement(rate) }
    }

    /** Closes and restarts the ranging session using the currently selected update rate. */
    fun restartRangingSession() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        val rate = repository.data.value.updateRate
        closeSession { startRangingMeasurement(rate) }
    }

    /**
     * Requests that the active ranging session stop and close. This method is not suspendable
     * and never blocks or delays - the actual teardown (and the [onClosed] parameter invocation)
     * happens asynchronously once [RangingSession.Callback] reports the session as closed.
     *
     * @param onClosed Optional action run once the session has fully closed. If omitted, the UI
     * state simply transitions to [RangingSessionAction.OnClosed].
     */
    @SuppressLint("MissingPermission") // Permission was already verified when the session was opened.
    fun closeSession(onClosed: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        val session = activeSession ?: run {
            onClosed?.invoke() ?: repository.updateSessionAction(RangingSessionAction.OnClosed)
            return
        }
        pendingRestart = onClosed
        closing = true
        if (onClosed != null) {
            repository.updateSessionAction(RangingSessionAction.OnRestarting)
        }
        try {
            if (isSessionOpen) session.stop() else session.close()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error closing ranging session")
            closing = false
            pendingRestart = null
            repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.UNKNOWN))
        }
    }

    /**
     * Starts a ranging session for this device with the requested update rate. If a session is
     * already active, this is a no-op - call [closeSession] first to change its rate.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun startRangingMeasurement(updateRate: UpdateRate = UpdateRate.NORMAL) {
        val rangingManager = rangingManager ?: run {
            repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.RANGING_NOT_AVAILABLE))
            return
        }
        if (activeSession != null) {
            Timber.tag(tag).w("Ranging session already active")
            return
        }
        currentRate = updateRate
        repository.updateRate(updateRate)

        val setRangingUpdateRate = when (updateRate) {
            UpdateRate.FREQUENT -> RawRangingDevice.UPDATE_RATE_FREQUENT
            UpdateRate.NORMAL -> RawRangingDevice.UPDATE_RATE_NORMAL
            UpdateRate.INFREQUENT -> RawRangingDevice.UPDATE_RATE_INFREQUENT
        }

        val rangingDevice = RangingDevice.Builder()
            .setUuid(UUID.nameUUIDFromBytes(deviceId.toByteArray()))
            .build()

        val csRangingParams = BleCsRangingParams
            .Builder(deviceId)
            .setRangingUpdateRate(setRangingUpdateRate)
            .setSecurityLevel(BleCsRangingCapabilities.CS_SECURITY_LEVEL_ONE)
            .build()

        val rawRangingDevice = RawRangingDevice.Builder()
            .setRangingDevice(rangingDevice)
            .setCsRangingParams(csRangingParams)
            .build()

        val rawRangingDeviceConfig = RawInitiatorRangingConfig.Builder()
            .addRawRangingDevice(rawRangingDevice)
            .build()

        val sensorFusionParams = SensorFusionParams.Builder()
            .setSensorFusionEnabled(true)
            .build()

        val sessionConfig = SessionConfig.Builder()
            .setRangingMeasurementsLimit(1000)
            .setAngleOfArrivalNeeded(true)
            .setSensorFusionParams(sensorFusionParams)
            .build()

        val rangingPreference = RangingPreference.Builder(DEVICE_ROLE_INITIATOR, rawRangingDeviceConfig)
            .setSessionConfig(sessionConfig)
            .build()

        val newCapabilitiesCallback = RangingManager.RangingCapabilitiesCallback { capabilities ->
            if (activeSession != null) return@RangingCapabilitiesCallback
            val csCapabilities = capabilities.csCapabilities
            when {
                csCapabilities == null -> {
                    repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.NOT_SUPPORTED))
                }

                !csCapabilities.supportedSecurityLevels.contains(BleCsRangingCapabilities.CS_SECURITY_LEVEL_ONE) -> {
                    repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.CS_SECURITY_NOT_AVAILABLE))
                }

                !hasRangingPermission() -> {
                    repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.MISSING_PERMISSION))
                }

                else -> openRangingSession(rangingManager, rangingPreference)
            }
        }
        capabilitiesCallback = newCapabilitiesCallback
        rangingManager.registerCapabilitiesCallback(context.mainExecutor, newCapabilitiesCallback)
    }

    /** Creates and starts the [RangingSession]. Permission was just verified by the caller. */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @SuppressLint("MissingPermission")
    private fun openRangingSession(
        rangingManager: RangingManager,
        rangingPreference: RangingPreference,
    ) {
        val session = rangingManager.createRangingSession(context.mainExecutor, createRangingSessionCallback())
        if (session == null) {
            repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.UNKNOWN))
            return
        }
        activeSession = session
        session.start(rangingPreference)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun createRangingSessionCallback() = object : RangingSession.Callback {
        override fun onOpened() {
            isSessionOpen = true
            repository.updateSessionAction(RangingSessionAction.OnStart)
        }

        override fun onOpenFailed(reason: Int) {
            activeSession = null
            unregisterCapabilitiesCallback()
            repository.updateSessionAction(RangingSessionAction.OnError(RangingSessionFailedReason.getReason(reason)))
        }

        override fun onStarted(peer: RangingDevice, technology: Int) {
            previousRangingData.clear()
            repository.updateSessionAction(RangingSessionAction.OnStart)
        }

        override fun onResults(peer: RangingDevice, data: RangingData) {
            data.distance?.measurement?.let { previousRangingData.add(it.toFloat()) }
            repository.updateSessionAction(
                RangingSessionAction.OnResult(
                    data = data.toCsRangingData(),
                    previousData = previousRangingData.toList(),
                )
            )
        }

        @SuppressLint("MissingPermission") // Permission was already verified when the session was opened.
        override fun onStopped(peer: RangingDevice, technology: Int) {
            isSessionOpen = false
            previousRangingData.clear()
            if (closing) {
                // We requested this stop as part of closeSession() - finish the teardown.
                activeSession?.close()
            } else {
                // The session stopped on its own (e.g. peer out of range); keep it around so a
                // restart can reuse it instead of tearing everything down.
                repository.updateSessionAction(RangingSessionAction.OnClosed)
            }
        }

        override fun onClosed(reason: Int) {
            activeSession = null
            closing = false
            isSessionOpen = false
            unregisterCapabilitiesCallback()
            previousRangingData.clear()

            val restart = pendingRestart
            pendingRestart = null
            when {
                restart != null -> restart()
                reason == RangingSessionFailedReason.LOCAL_REQUEST.reason ->
                    repository.updateSessionAction(RangingSessionAction.OnClosed)

                else ->
                    repository.updateSessionAction(RangingSessionAction.OnError(RangingSessionFailedReason.getReason(reason)))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun unregisterCapabilitiesCallback() {
        capabilitiesCallback?.let { rangingManager?.unregisterCapabilitiesCallback(it) }
        capabilitiesCallback = null
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun hasRangingPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.RANGING) == PackageManager.PERMISSION_GRANTED

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun RangingData.toCsRangingData(): CsRangingData = CsRangingData(
        distance = distance?.let {
            CSRangingMeasurement(measurement = it.measurement, confidenceLevel = ConfidenceLevel.from(it.confidence))
        },
        azimuth = azimuth?.let {
            CSRangingMeasurement(measurement = it.measurement, confidenceLevel = ConfidenceLevel.from(it.confidence))
        },
        elevation = elevation?.let {
            CSRangingMeasurement(measurement = it.measurement, confidenceLevel = ConfidenceLevel.from(it.confidence))
        },
        technology = RangingTechnology.from(rangingTechnology)
            ?: throw IllegalArgumentException("Unknown ranging technology: $rangingTechnology"),
        timeStamp = timestampMillis,
        hasRssi = hasRssi(),
        rssi = if (hasRssi()) rssi else null,
    )

    data class RasFeature(
        val realTimeRangingData: Boolean,
        val retrieveLostSegments: Boolean,
        val abortOperation: Boolean,
        val filterRangingData: Boolean,
    ) {
        override fun toString() = buildString {
            if (realTimeRangingData) append("Real Time Ranging Data, ")
            if (retrieveLostSegments) append("Retrieve Lost Segments, ")
            if (abortOperation) append("Abort Operation, ")
            if (filterRangingData) append("Filter Ranging Data, ")
        }.removeSuffix(", ").ifEmpty { "None" }
    }

    object RasFeatureParser {
        fun parse(data: ByteArray): RasFeature {
            require(data.size == 4) { "RAS Features characteristic must be 4 bytes." }
            val bits = (data[0].toInt() and 0xFF) or
                    ((data[1].toInt() and 0xFF) shl 8) or
                    ((data[2].toInt() and 0xFF) shl 16) or
                    ((data[3].toInt() and 0xFF) shl 24)
            return RasFeature(
                realTimeRangingData = bits and (1 shl 0) != 0,
                retrieveLostSegments = bits and (1 shl 1) != 0,
                abortOperation = bits and (1 shl 2) != 0,
                filterRangingData = bits and (1 shl 3) != 0,
            )
        }
    }
}
