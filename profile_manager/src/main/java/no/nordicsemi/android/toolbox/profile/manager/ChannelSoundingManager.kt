package no.nordicsemi.android.toolbox.profile.manager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.os.Build
import android.ranging.DataNotificationConfig
import android.ranging.RangingCapabilities
import android.ranging.RangingConfig
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
import android.ranging.ble.rssi.BleRssiRangingParams
import android.ranging.raw.RawInitiatorRangingConfig
import android.ranging.raw.RawRangingDevice
import android.ranging.wifi.pd.WifiPdRangingCapabilities
import android.ranging.wifi.rtt.RttRangingCapabilities
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.RANGING_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.data.AntennaMode
import no.nordicsemi.android.toolbox.profile.data.CSRangingMeasurement
import no.nordicsemi.android.toolbox.profile.data.CapabilityStatus
import no.nordicsemi.android.toolbox.profile.data.ConfidenceLevel
import no.nordicsemi.android.toolbox.profile.data.CsRangingData
import no.nordicsemi.android.toolbox.profile.data.HostCapabilities
import no.nordicsemi.android.toolbox.profile.data.LocationType
import no.nordicsemi.android.toolbox.profile.data.RangingOptions
import no.nordicsemi.android.toolbox.profile.data.RangingSessionAction
import no.nordicsemi.android.toolbox.profile.data.RangingSessionFailedReason
import no.nordicsemi.android.toolbox.profile.data.RangingTechnology
import no.nordicsemi.android.toolbox.profile.data.SessionClosedReason
import no.nordicsemi.android.toolbox.profile.data.SightType
import no.nordicsemi.android.toolbox.profile.data.TechnologyAvailability
import no.nordicsemi.android.toolbox.profile.data.UpdateRate
import no.nordicsemi.android.toolbox.profile.manager.repository.ChannelSoundingRepository
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.android.Peripheral as AndroidPeripheral
import no.nordicsemi.kotlin.ble.client.android.BondState
import timber.log.Timber
import java.util.UUID
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

// This flag is to test Ranging on phones without Channel Sounding.
// A RSSI measurement will be done instead if set.
private const val DEBUG_ALLOW_RSSI_RANGING = false

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
@Suppress("SimplifyBooleanWithConstants")
class ChannelSoundingManager(
    private val context: Context,
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(RANGING_SERVICE_UUID, deviceId, "Channel Sounding", onReady) {
    override val profile: ServiceType = ServiceType.CHANNEL_SOUNDING
    private val tag = "CS ($deviceId)"

    val repository = ChannelSoundingRepository()

    private lateinit var peripheral: Peripheral<*, *>
    private var rasFeaturesCharacteristic: RemoteCharacteristic? = null

    private val rangingManager: RangingManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            context.getSystemService(RangingManager::class.java)
        } else {
            null
        }
    }

    private var activeSession: RangingSession? = null
    private val previousRangingData = mutableListOf<Float>()

    /** Guards [requestHostCapabilitiesOnce] so host capabilities are fetched only once per connection. */
    private var capabilitiesRequested = false

    /** True while the session is open, i.e. between `onOpened` and `onStopped`/`onClosed`. */
    private var isSessionOpen = false

    /** True once [closeSession] requested a stop/close, until `RangingSession.Callback.onClosed` fires. */
    private var closing = false

    /** Action to run once the pending close (requested via [closeSession]) completes. */
    private var pendingRestart: (() -> Unit)? = null

    /** A flag set when accessing hidden Android API fails. No more attempts will be made. */
    private var hiddenApiBlocked = false

    override fun prepare(peripheral: Peripheral<*, *>, service: RemoteService) {
        this.peripheral = peripheral
        rasFeaturesCharacteristic = service.characteristics.firstOrNull { it.uuid == RAS_FEATURES }
    }

    override suspend fun CoroutineScope.initialize(peripheral: Peripheral<*, *>) {
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
                (peripheral as? AndroidPeripheral)?.bondState?.first { it == BondState.Bonded }
                requestHostCapabilitiesOnce()
            }
        }
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
            if (isSessionOpen) {
                Timber.tag(tag).v("Stopping session...")
                session.stop()
            } else {
                Timber.tag(tag).v("Closing session...")
                session.close()
            }
        } catch (e: Exception) {
            Timber.tag(tag).e("Operation failed: ${e.message}")
            closing = false
            pendingRestart = null
            repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.UNKNOWN))
        }
    }

    /**
     * Requests the host's ranging capabilities exactly once per connection and publishes them to
     * the UI. This does not start a session - it only populates the capabilities card. If Channel
     * Sounding is not supported at all, an error state is surfaced instead.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun requestHostCapabilitiesOnce() {
        if (capabilitiesRequested) return
        val rangingManager = rangingManager ?: run {
            repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.RANGING_NOT_AVAILABLE))
            return
        }
        capabilitiesRequested = true

        var callback: RangingManager.RangingCapabilitiesCallback? = null
        callback = RangingManager.RangingCapabilitiesCallback { capabilities ->
            callback?.let { rangingManager.unregisterCapabilitiesCallback(it) }
            Timber.tag(tag).log(Log.Level.APPLICATION, "Ranging capabilities:\n${RangingCapabilitiesPrinter.parse(capabilities)}")
            val host = capabilities.toHostCapabilities()
            repository.updateCapabilities(host)
            if (!host.channelSoundingSupported && !DEBUG_ALLOW_RSSI_RANGING) {
                Timber.tag(tag).w("Channel Sounding not supported by host")
                repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.NOT_SUPPORTED))
            }
        }.also {
            Timber.tag(tag).v("Requesting host capabilities...")
            rangingManager.registerCapabilitiesCallback(context.mainExecutor, it)
        }
    }

    /**
     * Starts a ranging session using the currently selected [RangingOptions]. If a session is
     * already active, this is a no-op. The configuration is validated against the previously
     * fetched host capabilities and the RANGING permission before the session is opened.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun startRanging() {
        val rangingManager = rangingManager ?: run {
            repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.RANGING_NOT_AVAILABLE))
            return
        }
        if (activeSession != null) {
            Timber.tag(tag).w("Ranging session already active")
            return
        }
        val capabilities = repository.data.value.capabilities ?: run {
            Timber.tag(tag).w("Host capabilities not available yet")
            return
        }
        when {
            !DEBUG_ALLOW_RSSI_RANGING && !capabilities.channelSoundingSupported ->
                repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.NOT_SUPPORTED))

            !DEBUG_ALLOW_RSSI_RANGING && BleCsRangingCapabilities.CS_SECURITY_LEVEL_ONE !in capabilities.supportedSecurityLevels -> {
                Timber.tag(tag).w("Security Level 1 not supported")
                repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.CS_SECURITY_NOT_AVAILABLE))
            }

            !hasRangingPermission() -> {
                Timber.tag(tag).w("Missing RANGING permission")
                repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.MISSING_PERMISSION))
            }

            else -> openRangingSession(rangingManager, buildRangingPreference(repository.data.value.config))
        }
    }

    /** Stops the active ranging session, if any, returning the UI to the idle (stopped) state. */
    fun stopRanging() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        Timber.tag(tag).log(Log.Level.APPLICATION, "Session stopped by user")
        closeSession()
    }

    /** Builds the [RangingPreference] for a Channel Sounding session from the given [config]. */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildRangingPreference(config: RangingOptions): RangingPreference {
        val setRangingUpdateRate = when (config.updateRate) {
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
            .setSightType(config.sightType.toAndroidSightType())
            .setLocationType(config.locationType.toAndroidLocationType())
            .setSecurityLevel(BleCsRangingCapabilities.CS_SECURITY_LEVEL_ONE)
            .build()

        val rawRangingDevice = RawRangingDevice.Builder()
            .setRangingDevice(rangingDevice)
            .setCsRangingParams(csRangingParams)
            .apply {
                if (DEBUG_ALLOW_RSSI_RANGING) {
                    setBleRssiRangingParams(BleRssiRangingParams.Builder(deviceId).build())
                }
            }
            .build()

        val rawRangingDeviceConfig = RawInitiatorRangingConfig.Builder()
            .addRawRangingDevice(rawRangingDevice)
            .build()

        val sensorFusionParams = SensorFusionParams.Builder()
            .setSensorFusionEnabled(config.sensorFusionEnabled)
            .build()

        val sessionConfig = SessionConfig.Builder()
            .setRangingMeasurementsLimit(0)
            .setAngleOfArrivalNeeded(false)
            .apply {
                // Antenna mode is only configurable from Android 17; UNSET leaves the system default.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
                    config.antennaMode != AntennaMode.UNSET
                ) {
                    setAntennaMode(config.antennaMode.toAndroidAntennaMode())
                }
            }
            .setSensorFusionParams(sensorFusionParams)
            .build()

        return RangingPreference
            .Builder(DEVICE_ROLE_INITIATOR, rawRangingDeviceConfig)
            .setSessionConfig(sessionConfig)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun SightType.toAndroidSightType(): Int = when (this) {
        SightType.UNKNOWN -> BleCsRangingParams.SIGHT_TYPE_UNKNOWN
        SightType.LINE_OF_SIGHT -> BleCsRangingParams.SIGHT_TYPE_LINE_OF_SIGHT
        SightType.NON_LINE_OF_SIGHT -> BleCsRangingParams.SIGHT_TYPE_NON_LINE_OF_SIGHT
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun LocationType.toAndroidLocationType(): Int = when (this) {
        LocationType.UNKNOWN -> BleCsRangingParams.LOCATION_TYPE_UNKNOWN
        LocationType.INDOOR -> BleCsRangingParams.LOCATION_TYPE_INDOOR
        LocationType.OUTDOOR -> BleCsRangingParams.LOCATION_TYPE_OUTDOOR
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun AntennaMode.toAndroidAntennaMode(): Int = when (this) {
        AntennaMode.OMNI -> SessionConfig.ANTENNA_MODE_OMNI
        AntennaMode.DIRECTIONAL -> SessionConfig.ANTENNA_MODE_DIRECTIONAL
        AntennaMode.UNSET -> SessionConfig.ANTENNA_MODE_OMNI // Never reached; UNSET is not applied.
    }

    /** Maps the system [RangingCapabilities] into the UI-facing [HostCapabilities] snapshot. */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun RangingCapabilities.toHostCapabilities(): HostCapabilities {
        val technologies = technologyAvailability.map { (technology, availability) ->
            TechnologyAvailability(
                technology = RangingTechnology.from(technology),
                rawValue = technology,
                status = availability.toCapabilityStatus(),
            )
        }
        return HostCapabilities(
            technologies = technologies,
            channelSoundingSupported = csCapabilities != null,
            supportedSecurityLevels = csCapabilities?.supportedSecurityLevels?.toList() ?: emptyList(),
        )
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun Int.toCapabilityStatus(): CapabilityStatus = when (this) {
        RangingCapabilities.ENABLED -> CapabilityStatus.ENABLED
        RangingCapabilities.NOT_SUPPORTED -> CapabilityStatus.NOT_SUPPORTED
        RangingCapabilities.DISABLED_USER -> CapabilityStatus.DISABLED_USER
        RangingCapabilities.DISABLED_REGULATORY -> CapabilityStatus.DISABLED_REGULATORY
        RangingCapabilities.DISABLED_USER_RESTRICTIONS -> CapabilityStatus.DISABLED_USER_RESTRICTIONS
        else -> CapabilityStatus.UNKNOWN
    }

    /** Creates and starts the [RangingSession]. Permission was just verified by the caller. */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @SuppressLint("MissingPermission")
    private fun openRangingSession(
        rangingManager: RangingManager,
        rangingPreference: RangingPreference,
    ) {
        Timber.tag(tag).v("Creating ranging session...")
        val session = rangingManager.createRangingSession(context.mainExecutor, createRangingSessionCallback())
        if (session == null) {
            Timber.tag(tag).w("Creating ranging session failed")
            repository.updateSessionAction(RangingSessionAction.OnError(SessionClosedReason.UNKNOWN))
            return
        }
        activeSession = session
        Timber.tag(tag).log(Log.Level.APPLICATION, "Starting session using:\n${RangingPreferencesPrinter.parse(rangingPreference)}")
        session.start(rangingPreference)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun createRangingSessionCallback() = object : RangingSession.Callback {
        override fun onOpened() {
            isSessionOpen = true
            repository.setRunning(true)
            Timber.tag(tag).log(Log.Level.APPLICATION, "Ranging session opened")
            repository.updateSessionAction(RangingSessionAction.OnStart)
        }

        override fun onOpenFailed(reason: Int) {
            activeSession = null
            repository.setRunning(false)
            Timber.tag(tag).e("Opening ranging session failed: $reason")
            repository.updateSessionAction(RangingSessionAction.OnError(RangingSessionFailedReason.getReason(reason)))
        }

        override fun onStarted(peer: RangingDevice, technology: Int) {
            Timber.tag(tag).log(Log.Level.APPLICATION, "Ranging session started with peer: ${peer.uuid} (technology: ${TechnologyParser.parseTechnology(technology)})")
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
            repository.setRunning(false)
            Timber.tag(tag).log(Log.Level.APPLICATION, "Ranging session stopped")
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
            repository.setRunning(false)
            if (reason == RangingSessionFailedReason.LOCAL_REQUEST.reason) {
                Timber.tag(tag).log(Log.Level.APPLICATION, "Ranging session closed (local request)")
            } else {
                Timber.tag(tag).e("Ranging session closed with reason: $reason")
            }
            previousRangingData.clear()

            val restart = pendingRestart
            pendingRestart = null
            when {
                restart != null -> {
                    Timber.tag(tag).v("Restarting session...")
                    restart()
                }

                reason == RangingSessionFailedReason.LOCAL_REQUEST.reason ->
                    repository.updateSessionAction(RangingSessionAction.OnClosed)

                else ->
                    repository.updateSessionAction(RangingSessionAction.OnError(RangingSessionFailedReason.getReason(reason)))
            }
        }
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
        rssi = if (hasRssi() && rssi != 127 /* DistanceMeasurementResult.INVALID_RSSI_DBM */) rssi else null,
        distanceStdDevMeters = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1 && hasDistanceStandardDeviation()) {
            distanceStandardDeviationMeters
        } else null,
        delaySpreadMeters = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            val fromExtras = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN) {
                rangingDataExtras?.bleSpecificData?.delaySpreadMeters
            } else null
            fromExtras ?: if (hiddenApiBlocked) null
            else try { delaySpreadMetersCompat } catch (_: NoSuchMethodException) { hiddenApiBlocked = true; null }
        } else null,
        velocityMetersPerSec = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1 && !hiddenApiBlocked) {
            try { velocityMetersPerSecCompat } catch (_: NoSuchMethodException) { hiddenApiBlocked = true; null }
        } else null,
        detectedAttackLevelPercent = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1 && !hiddenApiBlocked) {
            try { detectedAttackLevelCompat } catch (_: NoSuchMethodException) { hiddenApiBlocked = true; null }
        } else null,
        remoteTxPowerDbm = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN) {
            val power = rangingDataExtras?.bleSpecificData?.remoteTxPowerDbm
            if (power == 127 /* DistanceMeasurementResult.INVALID_RSSI_DBM */) null else power
        } else null,
    )

    val RangingData.delaySpreadMetersCompat: Double?
        @SuppressLint("PrivateApi") @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        get() = try {
            val method = RangingData::class.java.getDeclaredMethod("getDelaySpreadMeters")
            method.isAccessible = true
            val delaySpread = method.invoke(this) as Double
            if (delaySpread.isNaN()) null else delaySpread
        } catch (e: NoSuchMethodException) {
            throw e
        } catch (_: Exception) {
            null // Default to null if not accessible
        }

    val RangingData.velocityMetersPerSecCompat: Double?
        @SuppressLint("PrivateApi") @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        get() = try {
            val method = RangingData::class.java.getDeclaredMethod("getVelocityMetersPerSec")
            method.isAccessible = true
            val velocity = method.invoke(this) as Double
            if (velocity.isNaN()) null else velocity
        } catch (e: NoSuchMethodException) {
            throw e
        } catch (_: Exception) {
            null // Default to null if not accessible
        }

    val RangingData.detectedAttackLevelCompat: Int?
        @SuppressLint("PrivateApi") @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        get() = try {
            val method = RangingData::class.java.getDeclaredMethod("getDetectedAttackLevel")
            method.isAccessible = true
            val attack = method.invoke(this) as Byte
            if (attack == 0xFF.toByte()) null else attack.toInt()
        } catch (e: NoSuchMethodException) {
            throw e
        } catch (_: Exception) {
            null // Default to null if not accessible
        }

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

    private object RasFeatureParser {
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

    private object TechnologyParser {

        fun parseTechnology(technology: Int) = when (technology) {
            RangingManager.UWB -> "Ultra-Wideband (UWB)"
            RangingManager.BLE_CS -> "Channel Sounding (CS)"
            RangingManager.WIFI_NAN_RTT -> "Wi-Fi NAN RTT" // Neighborhood Aware Networking
            RangingManager.BLE_RSSI -> "RSSI"
            RangingManager.WIFI_STA_RTT -> "Wi-Fi STA-AP RTT" // Stationary - Access Point
            RangingManager.WIFI_PD -> "Wi-Fi Proximity Detection (PD)" // Proximity Detection
            else -> "Unknown ($technology)"
        }
    }

    private object RangingCapabilitiesPrinter {
        /** A flag set when accessing hidden Android API fails. No more attempts will be made. */
        private var hiddenApiBlocked = false

        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun parse(capabilities: RangingCapabilities) = buildString {
            appendLine("● Available technologies:")
            capabilities.technologyAvailability.forEach { (technology, isAvailable) ->
                appendLine("  - ${TechnologyParser.parseTechnology(technology)}: ${parseAvailability(isAvailable)}")
            }
            capabilities.uwbCapabilities?.let {
                appendLine("● Ultra-Wideband (UWB)")
                append("  - Distance Measurement Supported: ")
                appendLine(it.isDistanceMeasurementSupported)
                append("  - Azimuthal Angle Supported: ")
                appendLine(it.isAzimuthalAngleSupported)
                append("  - Elevation Angle Supported: ")
                appendLine(it.isElevationAngleSupported)
                append("  - Ranging Interval Reconfigure Supported: ")
                appendLine(it.isRangingIntervalReconfigurationSupported)
                append("  - Min Ranging Interval: ")
                appendLine(it.minimumRangingInterval)
                append("  - Supported Channels: ")
                appendLine(it.supportedChannels.joinToString())
                append("  - Supported Notification Configurations: ")
                appendLine(it.supportedNotificationConfigurations.joinToString())
                append("  - Supported Config IDs: ")
                appendLine(it.supportedConfigIds.joinToString())
                append("  - Supported Slot Durations: ")
                appendLine(it.supportedSlotDurations.joinToString())
                append("  - Supported Ranging Update Rates: ")
                appendLine(it.supportedRangingUpdateRates.joinToString())
                append("  - Supported Preamble Indexes: ")
                appendLine(it.supportedPreambleIndexes.joinToString())
                append("  - Background Ranging Supported: ")
                appendLine(it.isBackgroundRangingSupported)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                    append("  - DL-TDOA Supported: ")
                    appendLine(it.isDlTdoaSupported)
                    append("  - Supported Antenna Modes: ")
                    appendLine(it.supportedAntennaModes.joinToString())
                }
                appendLine()
            }
            capabilities.csCapabilities?.let { csCapabilities ->
                appendLine("● Channel Sounding")
                append("  - Security Levels: ")
                appendLine(csCapabilities.supportedSecurityLevels.joinToString())
            }
            capabilities.rttRangingCapabilities?.let { rttRangingCapabilities ->
                appendLine("● Round Trip Time (RTT)")
                append("  - Periodic Ranging Hardware Feature: ")
                appendLine(rttRangingCapabilities.hasPeriodicRangingHardwareFeature())
                try {
                    if (hiddenApiBlocked) return@let
                    rttRangingCapabilities.maxSupportedBandwidthCompat?.let { maxSupportedBandwidth ->
                        append("  - Max Supported Bandwidth: ")
                        appendLine(maxSupportedBandwidth)
                    }
                    rttRangingCapabilities.maxSupportedRxChainCompat?.let { maxSupportedRxChain ->
                        append("  - Max Supported RX Chain: ")
                        appendLine(maxSupportedRxChain)
                    }
                } catch (_: NoSuchMethodException) {
                    hiddenApiBlocked = true
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                capabilities.wifiPdRangingCapabilities?.let {
                    appendLine("● Wi-Fi Proximity Detection (PD)")
                    append("  - Supported PASN Modes: ")
                    appendLine(it.supportedPasnModes.joinToString { mode -> parsePasnMode(mode) })
                    append("  - Proximity Detection MAC Address: ")
                    appendLine(it.proximityDetectionMacAddress.toString())
                    append("  - Wi-Fi RTT Supported: ")
                    appendLine(it.is80211mcSupported)
                    append("  - 802.11az NTB (Next Generation Trigger-Based) Supported: ")
                    appendLine(it.is80211azNtbSupported)
                    append("  - Max Channel Width: ")
                    appendLine(parseChannelWidth(it.maxChannelWidth))
                    append("  - Max Preamble: ")
                    appendLine(parsePreamble(it.maxPreamble))
                    append("  - Supported Discovery Channel Frequencies (MHz): ")
                    appendLine(it.supportedDiscoveryChannelFrequenciesMhz.joinToString { freq ->"$freq MHz" })
                    append("  - 802.11mc Min Ranging Interval: ")
                    appendLine(it.get80211mcMinRangingInterval())
                    append("  - 802.11az NTB Min Ranging Interval: ")
                    appendLine(it.get80211azNtbMinRangingInterval())
                }
            }
        }.removeSuffix("\n")

        private fun parseAvailability(availability: Int) = when (availability) {
            RangingCapabilities.NOT_SUPPORTED -> "Not Supported"
            RangingCapabilities.DISABLED_USER -> "Disabled (User)"
            RangingCapabilities.DISABLED_REGULATORY -> "Disabled (Regulatory)"
            RangingCapabilities.ENABLED -> "Enabled"
            RangingCapabilities.DISABLED_USER_RESTRICTIONS -> "Disabled (User Restrictions)"
            else -> "Unknown ($availability)"
        }

        private fun parsePasnMode(mode: Int) = when (mode) {
            WifiPdRangingCapabilities.UNAUTHENTICATED_PASN_MODE -> "Unauthenticated"
            WifiPdRangingCapabilities.AUTHENTICATED_PASN_MODE -> "Authenticated"
            else -> "Unknown ($mode)"
        }

        private fun parseChannelWidth(channelWidth: Int) = when (channelWidth) {
            ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
            ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
            ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
            ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
            ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80 MHz + 80 MHz"
            ScanResult.CHANNEL_WIDTH_320MHZ -> "320 MHz"
            else -> "Unknown ($channelWidth)"
        }

        private fun parsePreamble(preamble: Int) = when (preamble) {
            ScanResult.PREAMBLE_LEGACY -> "Legacy"
            ScanResult.PREAMBLE_HT -> "HT"
            ScanResult.PREAMBLE_VHT -> "VHT"
            ScanResult.PREAMBLE_HE -> "HE"
            ScanResult.PREAMBLE_EHT -> "EHT"
            else -> "Unknown ($preamble)"
        }

        /**
         * Extension property to access the hidden `maxSupportedBandwidth` field in [RttRangingCapabilities].
         */
        val RttRangingCapabilities.maxSupportedBandwidthCompat: Int?
            @SuppressLint("PrivateApi") @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            get() = try {
                val method =
                    RttRangingCapabilities::class.java.getDeclaredMethod("getMaxSupportedBandwidth")
                method.isAccessible = true
                val bandwidth = method.invoke(this) as Int
                if (bandwidth == -1) null else bandwidth
            } catch (e: NoSuchMethodException) {
                throw e
            } catch (_: Exception) {
                null // Default to null if not accessible
            }

        /**
         * Extension property to access the hidden `maxSupportedRxChain` field in [RttRangingCapabilities].
         */
        val RttRangingCapabilities.maxSupportedRxChainCompat: Int?
            @SuppressLint("PrivateApi") @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            get() = try {
                val method = RttRangingCapabilities::class.java.getDeclaredMethod("getMaxSupportedRxChain")
                method.isAccessible = true
                method.invoke(this) as Int
            } catch (e: NoSuchMethodException) {
                throw e
            } catch (_: Exception) {
                null // Default to null if not accessible
            }
    }

    private object RangingPreferencesPrinter {

        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun parse(preferences: RangingPreference) = buildString {
            append("● Device Role: ")
            appendLine(parseDeviceRole(preferences.deviceRole))
            preferences.rangingParams?.let { config ->
                appendLine("● Ranging Parameters:")
                append("  - Type: ")
                appendLine(parseRangingSessionType(config.rangingSessionType))

                (config as? RawInitiatorRangingConfig)?.let { initiatorConfig ->
                    appendLine("  - Devices: ")
                    appendLine(initiatorConfig.rawRangingDevices.joinToString { device -> RawRandingDevicePrinter.parse(device) })
                }
            }
            preferences.sessionConfig.let { session ->
                appendLine("● Session Configuration:")
                append("  - Angle of Arrival Needed: ")
                appendLine(session.isAngleOfArrivalNeeded)
                append("  - Ranging Measurements Limit: ")
                appendLine(session.rangingMeasurementsLimit)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                    append("  - Antenna Mode: ")
                    appendLine(parseAntennaMode(session.antennaMode))
                }
                session.sensorFusionParams.let { params ->
                    append("  - Sensor Fusion Enabled: ")
                    appendLine(params.isSensorFusionEnabled)
                }
                session.dataNotificationConfig.let { config ->
                    append("  - Data Notifications: ")
                    appendLine(parseNotificationType(config.notificationConfigType))
                    append("  - Proximity Near: ")
                    append(config.proximityNearCm).appendLine(" cm")
                    append("  - Proximity Far: ")
                    append(config.proximityFarCm).appendLine(" cm")
                }
            }
        }.removeSuffix("\n")

        private fun parseDeviceRole(role: Int) = when (role) {
            /* RangingPreference.DEVICE_ROLE_RESPONDER */ 0 -> "Responder"
            /* RangingPreference.DEVICE_ROLE_INITIATOR */ 1 -> "Initiator"
            /* RangingPreference.DEVICE_ROLE_DT_TAG */ 2 -> "DT Tag"
            else -> "Unknown ($role)"
        }

        private fun parseRangingSessionType(type: Int) = when (type) {
            RangingConfig.RANGING_SESSION_RAW -> "RAW"
            RangingConfig.RANGING_SESSION_OOB -> "OOB"
            else -> "Unknown ($type)"
        }

        @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
        private fun parseAntennaMode(mode: Int) = when (mode) {
            SessionConfig.ANTENNA_MODE_OMNI -> "Omni"
            SessionConfig.ANTENNA_MODE_DIRECTIONAL -> "Directional"
            /* SessionConfig.ANTENNA_MODE_UNSET */ 2 -> "Unset"
            else -> "Unknown ($mode)"
        }

        private fun parseNotificationType(type: Int) = when (type) {
            // Range data notification will be disabled.
            DataNotificationConfig.NOTIFICATION_CONFIG_DISABLE -> "Disabled"
            // Range data notification will be enabled (default).
            DataNotificationConfig.NOTIFICATION_CONFIG_ENABLE -> "Enabled"
            // Range data notification is enabled when peer device is in the configured range - [near, far].
            DataNotificationConfig.NOTIFICATION_CONFIG_PROXIMITY_LEVEL -> "Proximity Level"
            // Range data notification is enabled when peer device enters or exits the configured range - [near, far].
            DataNotificationConfig.NOTIFICATION_CONFIG_PROXIMITY_EDGE -> "Proximity Edge"
            else -> "Unknown ($type)"
        }
    }

    private object RawRandingDevicePrinter {

        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun parse(device: RawRangingDevice) = buildString {
            var addressPrinted = false
            append("    ○ ID: ")
            appendLine(device.rangingDevice.uuid)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                device.rangingDevice.dlTdoaUwbAddress?.let {
                    append("       DL-TDOA UWB Address: ")
                    appendLine(it)
                }
            }
            device.csRangingParams?.let { csParams ->
                append("       Address: ")
                appendLine(csParams.peerBluetoothAddress)
                addressPrinted = true
                append("       Security Level: ")
                appendLine(csParams.securityLevel)
                append("       Location: ")
                appendLine(parseLocationType(csParams.locationType))
                append("       Sight Type: ")
                appendLine(parseSightType(csParams.sightType))
                append("       CS Ranging Update Rate: ")
                appendLine(parseRangingRate(csParams.rangingUpdateRate))
            }
            device.bleRssiRangingParams?.let { rssiParams ->
                if (!addressPrinted) {
                    append("       Address: ")
                    appendLine(rssiParams.peerBluetoothAddress)
                    addressPrinted = true
                }
                append("       RSSI Ranging Update Rate: ")
                appendLine(parseRangingRate(rssiParams.rangingUpdateRate))
            }
            device.rttRangingParams?.let { rtt ->
                append("       Service Name: ")
                appendLine(rtt.serviceName)
                rtt.matchFilter?.let {
                    append("       Match Filter: 0x")
                    appendLine(it.toHexString())
                }
                append("       RTT Ranging Update Rate: ")
                appendLine(parseRangingRate(rtt.rangingUpdateRate))
                append("       Periodic Ranging HW Feature Enabled: ")
                appendLine(rtt.isPeriodicRangingHwFeatureEnabled)
            }
            // TODO Add other technologies
        }.removeSuffix("\n")

        private fun parseLocationType(type: Int) = when (type) {
            BleCsRangingParams.LOCATION_TYPE_UNKNOWN -> "Unknown"
            BleCsRangingParams.LOCATION_TYPE_INDOOR -> "Indoor"
            BleCsRangingParams.LOCATION_TYPE_OUTDOOR -> "Outdoor"
            else -> "Unknown ($type)"
        }

        private fun parseSightType(type: Int) = when (type) {
            BleCsRangingParams.SIGHT_TYPE_UNKNOWN -> "Unknown"
            BleCsRangingParams.SIGHT_TYPE_LINE_OF_SIGHT -> "Line of Sight"
            BleCsRangingParams.SIGHT_TYPE_NON_LINE_OF_SIGHT -> "Non-Line of Sight"
            else -> "Unknown ($type)"
        }

        private fun parseRangingRate(rate: Int) = when (rate) {
            RawRangingDevice.UPDATE_RATE_NORMAL -> "Normal"
            RawRangingDevice.UPDATE_RATE_INFREQUENT -> "Infrequent"
            RawRangingDevice.UPDATE_RATE_FREQUENT -> "Frequent"
            else -> "Unknown ($rate)"
        }
    }
}
