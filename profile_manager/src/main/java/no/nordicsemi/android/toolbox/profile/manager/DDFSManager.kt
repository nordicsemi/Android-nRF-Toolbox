package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.DDF_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.DFSRepository
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.azimuthal.AzimuthalMeasurementDataParser
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointDataParser
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointMode
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.ddf.DDFDataParser
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.DistanceMeasurementDataParser
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.elevation.ElevationMeasurementDataParser
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val DISTANCE_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("21490001-494a-4573-98af-f126af76f490")
private val AZIMUTH_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("21490002-494a-4573-98af-f126af76f490")
private val ELEVATION_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("21490003-494a-4573-98af-f126af76f490")
private val DDF_FEATURE_CHARACTERISTIC_UUID = Uuid.parse("21490004-494a-4573-98af-f126af76f490")
private val CONTROL_POINT_CHARACTERISTIC_UUID = Uuid.parse("21490005-494a-4573-98af-f126af76f490")

private val MCPD_ENABLED_BYTES = byteArrayOf(0x01, 0x01)
private val RTT_ENABLED_BYTES = byteArrayOf(0x01, 0x00)
private val CHECK_CONFIG_BYTES = byteArrayOf(0x0A)

internal class DDFSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(DDF_SERVICE_UUID, deviceId, "DDFS", onReady) {
    override val profile: ServiceType = ServiceType.DDFS

    private var azimuthCharacteristic: RemoteCharacteristic? = null
    private var distanceCharacteristic: RemoteCharacteristic? = null
    private var elevationCharacteristic: RemoteCharacteristic? = null
    private var ddfFeatureCharacteristic: RemoteCharacteristic? = null
    private lateinit var controlPointCharacteristic: RemoteCharacteristic

    override fun prepare(service: RemoteService) {
        controlPointCharacteristic = service.characteristics.first { it.uuid == CONTROL_POINT_CHARACTERISTIC_UUID }
        require(controlPointCharacteristic.isWritable()) { "DFS control point characteristic must be writable" }
        azimuthCharacteristic = service.characteristics.firstOrNull { it.uuid == AZIMUTH_MEASUREMENT_CHARACTERISTIC_UUID }
        distanceCharacteristic = service.characteristics.firstOrNull { it.uuid == DISTANCE_MEASUREMENT_CHARACTERISTIC_UUID }
        elevationCharacteristic = service.characteristics.firstOrNull { it.uuid == ELEVATION_MEASUREMENT_CHARACTERISTIC_UUID }
        ddfFeatureCharacteristic = service.characteristics.firstOrNull { it.uuid == DDF_FEATURE_CHARACTERISTIC_UUID }
    }

    override suspend fun CoroutineScope.initialize() {
        azimuthCharacteristic?.subscribe()
            ?.mapNotNull { AzimuthalMeasurementDataParser().parse(it) }
            ?.onEach {
                Timber.tag("DDFS").log(Log.Level.APPLICATION, it.toString())
                DFSRepository.addNewAzimuth(deviceId, it)
            }
            ?.catch { Timber.tag("DDFS").e(it) }
            ?.onCompletion { DFSRepository.clear(deviceId) }
            ?.launchIn(this)

        distanceCharacteristic?.subscribe()
            ?.mapNotNull { DistanceMeasurementDataParser().parse(it) }
            ?.onEach {
                Timber.tag("DFS").log(Log.Level.APPLICATION, it.toString())
                DFSRepository.addNewDistance(deviceId, it)
            }
            ?.catch { Timber.tag("DDFS").e(it) }
            ?.onCompletion { DFSRepository.clear(deviceId) }
            ?.launchIn(this)

        elevationCharacteristic?.subscribe()
            ?.mapNotNull { ElevationMeasurementDataParser().parse(it) }
            ?.onEach {
                Timber.tag("DDFS").log(Log.Level.APPLICATION, it.toString())
                DFSRepository.addNewElevation(deviceId, it)
            }
            ?.catch { Timber.tag("DDFS").e(it) }
            ?.onCompletion { DFSRepository.clear(deviceId) }
            ?.launchIn(this)

        controlPointCharacteristic.subscribe()
            .mapNotNull { ControlPointDataParser().parse(it) }
            .onEach {
                Timber.tag("DDFS").log(Log.Level.APPLICATION, it.toString())
                DFSRepository.onControlPointDataReceived(deviceId, it, this)
            }
            .catch { Timber.tag("DFS").e(it) }
            .onCompletion { DFSRepository.clear(deviceId) }
            .launchIn(this)

        ddfFeatureCharacteristic?.let { char ->
            launch {
                try {
                    if (char.isReadable()) {
                        Timber.tag("DDFS").v("Reading DDF features...")
                        DDFDataParser().parse(char.read())
                            ?.also {
                                Timber.tag("DFS").log(Log.Level.APPLICATION, it.toString())
                                DFSRepository.setAvailableDistanceModes(deviceId, it)
                            }
                    }
                } catch (e: Exception) {
                    Timber.tag("DDFS").e(e, "Error reading DDF features")
                }
            }
        }

        DFSRepository.registerManager(deviceId, this@DDFSManager)
        onReady(this@DDFSManager)
    }

    suspend fun enableDistanceMode(mode: ControlPointMode) {
        val data = when (mode) {
            ControlPointMode.MCPD -> MCPD_ENABLED_BYTES
            ControlPointMode.RTT -> RTT_ENABLED_BYTES
        }
        try {
            Timber.tag("DDFS").v("Enabling distance mode: $mode")
            controlPointCharacteristic.write(data)
            Timber.tag("DDFS").log(Log.Level.APPLICATION, "$mode enabled")
            DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
        } catch (e: Exception) {
            Timber.tag("DDFS").e(e, "Failed to enable $mode mode")
            DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.FAILED)
        }
    }

    suspend fun checkForCurrentDistanceMode() {
        try {
            Timber.tag("DDFS").v("Checking current distance mode...")
            controlPointCharacteristic.write(CHECK_CONFIG_BYTES)
            DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
        } catch (e: Exception) {
            Timber.tag("DDFS").e(e, "Failed to check current distance mode")
            DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.FAILED)
        }
    }

    suspend fun checkAvailableFeatures() {
        DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.PENDING)
        val char = ddfFeatureCharacteristic ?: return
        if (char.isReadable()) {
            try {
                Timber.tag("DDFS").v("Reading DDF features...")
                DDFDataParser().parse(char.read())?.let {
                    Timber.log(Log.Level.APPLICATION, "Features: $it")
                    DFSRepository.setAvailableDistanceModes(deviceId, it)
                }
            } catch (e: Exception) {
                Timber.tag("DDFS").e(e, "Error checking available features")
            }
        }
    }
}
