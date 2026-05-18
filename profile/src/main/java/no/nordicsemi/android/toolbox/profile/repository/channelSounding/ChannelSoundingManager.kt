package no.nordicsemi.android.toolbox.profile.repository.channelSounding

import android.Manifest
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
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.toolbox.profile.data.ChannelSoundingServiceData
import no.nordicsemi.android.toolbox.profile.data.RangingSessionAction
import no.nordicsemi.android.toolbox.profile.data.RangingSessionFailedReason
import no.nordicsemi.android.toolbox.profile.data.SessionClosedReason
import no.nordicsemi.android.toolbox.profile.data.UpdateRate
import no.nordicsemi.android.toolbox.profile.view.channelSounding.toCsRangingData
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChannelSoundingManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private val rangingManager: RangingManager? =
        context.getSystemService(RangingManager::class.java)
    private val _dataMap = mutableMapOf<String, MutableStateFlow<ChannelSoundingServiceData>>()
    private val _activeSessions = mutableMapOf<String, RangingSession>()
    private val _capabilityCallbacks =
        mutableMapOf<String, RangingManager.RangingCapabilitiesCallback>()
    private val _previousRangingData = mutableMapOf<String, MutableList<Float>>()

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun createRangingSessionCallback(deviceAddress: String) =
        object : RangingSession.Callback {
            override fun onClosed(reason: Int) {
                updateRangingData(
                    deviceAddress,
                    RangingSessionAction.OnError(RangingSessionFailedReason.getReason(reason))
                )
                cleanUpDeviceSession(deviceAddress)
            }

            override fun onOpenFailed(reason: Int) {
                updateRangingData(
                    deviceAddress,
                    RangingSessionAction.OnError(RangingSessionFailedReason.getReason(reason))
                )
                cleanUpDeviceSession(deviceAddress)
            }

            override fun onOpened() {
                updateRangingData(deviceAddress, RangingSessionAction.OnStart)
            }

            override fun onResults(peer: RangingDevice, data: RangingData) {
                val history = _previousRangingData.getOrPut(deviceAddress) { mutableListOf() }
                data.distance?.measurement?.let {
                    history.add(it.toFloat())
                }

                updateRangingData(
                    deviceAddress,
                    RangingSessionAction.OnResult(
                        data = data.toCsRangingData(),
                        previousData = history.toList()
                    )
                )
            }

            override fun onStarted(peer: RangingDevice, technology: Int) {
                updateRangingData(deviceAddress, RangingSessionAction.OnStart)
                _previousRangingData[deviceAddress]?.clear()
            }

            override fun onStopped(peer: RangingDevice, technology: Int) {
                updateRangingData(deviceAddress, RangingSessionAction.OnClosed)
                _previousRangingData[deviceAddress]?.clear()
            }
        }

    /**
     * Returns a [Flow] of [ChannelSoundingServiceData] for the given device ID.
     * If no data exists for the device, a new [MutableStateFlow] with default [ChannelSoundingServiceData] is created.
     */
    fun getData(deviceId: String): Flow<ChannelSoundingServiceData> {
        return _dataMap.getOrPut(deviceId) { MutableStateFlow(ChannelSoundingServiceData()) }
    }

    /**
     * Adds a device to the ranging session and starts the session if not already active.
     * If the session is already active, it continues the session.
     * If the RangingManager is not available or permissions are missing, it updates the state with an error.
     * Requires Android version Baklava (API 36) or higher.
     *
     * @param device The device address to add to the ranging session.
     * @param updateRate The desired update rate for ranging measurements. Default is [UpdateRate.NORMAL].
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun startRangingMeasurement(
        device: String,
        updateRate: UpdateRate = UpdateRate.NORMAL
    ) {
        if (rangingManager == null) {
            updateRangingData(
                device,
                RangingSessionAction.OnError(SessionClosedReason.RANGING_NOT_AVAILABLE)
            )
            return
        }
        // If session is already active for the device then continue the session, otherwise create a new one.
        if (_activeSessions.containsKey(device)) {
            Timber.w("Ranging session already active for device: $device")
            return
        }
        val setRangingUpdateRate = when (updateRate) {
            UpdateRate.FREQUENT -> RawRangingDevice.UPDATE_RATE_FREQUENT
            UpdateRate.NORMAL -> RawRangingDevice.UPDATE_RATE_NORMAL
            UpdateRate.INFREQUENT -> RawRangingDevice.UPDATE_RATE_INFREQUENT
        }
        val rangingDevice = RangingDevice.Builder()
            .setUuid(UUID.nameUUIDFromBytes(device.toByteArray()))
            .build()

        val csRangingParams = BleCsRangingParams
            .Builder(device)
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


        val rangingPreference = RangingPreference.Builder(
            DEVICE_ROLE_INITIATOR,
            rawRangingDeviceConfig
        )
            .setSessionConfig(sessionConfig)
            .build()

        val rangingCapabilityCallback = RangingManager.RangingCapabilitiesCallback { capabilities ->
            if (capabilities.csCapabilities != null) {
                if (capabilities.csCapabilities!!.supportedSecurityLevels.contains(1)) {
                    if (hasRangingPermissions(context)) {

                        val session = rangingManager.createRangingSession(
                            context.mainExecutor,
                            createRangingSessionCallback(device) // Dynamic device callback
                        )

                        session?.let {
                            try {
                                _activeSessions[device] = it
                                it.addDeviceToRangingSession(rawRangingDeviceConfig)
                            } catch (e: Exception) {
                                Timber.e("Failed to add device to ranging session: ${e.message}")
                                updateRangingData(device, RangingSessionAction.OnClosed)
                            } finally {
                                it.start(rangingPreference)
                            }
                        } ?: run {
                            updateRangingData(
                                device,
                                RangingSessionAction.OnError(SessionClosedReason.UNKNOWN)
                            )
                            return@RangingCapabilitiesCallback
                        }
                    } else {
                        updateRangingData(
                            device,
                            RangingSessionAction.OnError(SessionClosedReason.MISSING_PERMISSION)
                        )
                        return@RangingCapabilitiesCallback
                    }
                } else {
                    updateRangingData(
                        device,
                        RangingSessionAction.OnError(SessionClosedReason.CS_SECURITY_NOT_AVAILABLE)
                    )
                    closeSession(device)
                }
            } else {
                updateRangingData(
                    device,
                    RangingSessionAction.OnError(SessionClosedReason.NOT_SUPPORTED)
                )
                closeSession(device)
            }
        }

        _capabilityCallbacks[device] = rangingCapabilityCallback
        rangingManager.registerCapabilitiesCallback(context.mainExecutor, rangingCapabilityCallback)
    }

    /**
     * Closes the current ranging session if it exists.
     * Waits for the session to stop before closing it and unregistering the capabilities callback.
     * If onClosed is provided, it will be called after the session is closed.
     * Requires Android version Baklava (API 36) or higher.
     *
     * @param deviceAddress The address of the device associated with the ranging session.
     * @param onClosed An optional suspend function to be called after the session is closed.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun closeSession(
        deviceAddress: String,
        onClosed: (suspend () -> Unit)? = null
    ) {
        val session = _activeSessions[deviceAddress] ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onClosed?.let {
                    updateRangingData(deviceAddress, RangingSessionAction.OnRestarting)
                }
                session.stop()
                // Wait for onStopped() or onClosed() before closing
                delay(1000) // Give the system time to propagate onStopped
                withContext(Dispatchers.Main) {
                    session.close()
                    cleanUpDeviceSession(deviceAddress)
                    delay(1500)
                    onClosed?.let { it() } ?: run {
                        clear(deviceAddress)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error closing ranging session for $deviceAddress")
                updateRangingData(
                    deviceAddress,
                    RangingSessionAction.OnError(SessionClosedReason.UNKNOWN)
                )
            }
        }
    }

    /**
     * Shared helper to cleanly tear down session maps for a specific device.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun cleanUpDeviceSession(deviceAddress: String) {
        _activeSessions.remove(deviceAddress)
        _previousRangingData.remove(deviceAddress)
        _capabilityCallbacks.remove(deviceAddress)?.let { callback ->
            rangingManager?.unregisterCapabilitiesCallback(callback)
        }
    }

    /**
     * Checks if the app has the RANGING permission.
     * Requires Android version Baklava (API 36) or higher.
     *
     * @param context The context to use for checking permissions.
     * @return True if the RANGING permission is granted, false otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun hasRangingPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RANGING
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Clears the data associated with the given device ID.
     * This removes the entry from the internal map, effectively resetting the state for that device.
     *
     * @param deviceId The ID of the device whose data should be cleared.
     */
    fun clear(deviceId: String) = _dataMap.remove(deviceId)

    /**
     * Updates the ranging session action for the specified device.
     *
     * @param deviceId The ID of the device to update.
     * @param rangingData The new ranging session action to set.
     */
    fun updateRangingData(deviceId: String, rangingData: RangingSessionAction) =
        _dataMap[deviceId]?.update { it.copy(rangingSessionAction = rangingData) }

    /**
     * Updates the ranging update rate for the specified device.
     *
     * @param address The ID of the device to update.
     * @param frequency The new update rate to set.
     */
    fun updateRangingRate(address: String, frequency: UpdateRate) =
        _dataMap[address]?.update { it.copy(updateRate = frequency) }

    /**
     * Updates the interval rate for the specified device.
     *
     * @param address The ID of the device to update.
     * @param interval The new interval rate to set.
     */
    fun updateIntervalRate(address: String, interval: Int) =
        _dataMap[address]?.update { it.copy(interval = interval) }

}
