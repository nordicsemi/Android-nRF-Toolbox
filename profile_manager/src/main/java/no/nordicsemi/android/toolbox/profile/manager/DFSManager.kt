@file:OptIn(ExperimentalUuidApi::class)

package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.lib.utils.logAndReport
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
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty
import no.nordicsemi.kotlin.ble.core.WriteType
import timber.log.Timber
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val DISTANCE_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("21490001-494a-4573-98af-f126af76f490")
private val AZIMUTH_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("21490002-494a-4573-98af-f126af76f490")
private val ELEVATION_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("21490003-494a-4573-98af-f126af76f490")
private val DDF_FEATURE_CHARACTERISTIC_UUID = Uuid.parse("21490004-494a-4573-98af-f126af76f490")
private val CONTROL_POINT_CHARACTERISTIC_UUID = Uuid.parse("21490005-494a-4573-98af-f126af76f490")

internal class DFSManager : ServiceManager {
    override val profile: Profile
        get() = Profile.DFS

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun observeServiceInteractions(
        deviceId: String,
        remoteService: RemoteService,
        scope: CoroutineScope
    ) {
        remoteService.characteristics
            .firstOrNull { it.uuid == AZIMUTH_MEASUREMENT_CHARACTERISTIC_UUID }
            ?.subscribe()
            ?.mapNotNull { AzimuthalMeasurementDataParser().parse(it) }
            ?.onEach { DFSRepository.addNewAzimuth(deviceId, it) }
            ?.catch { it.logAndReport() }
            ?.onCompletion { DFSRepository.clear(deviceId) }
            ?.launchIn(scope)

        remoteService.characteristics
            .firstOrNull { it.uuid == DISTANCE_MEASUREMENT_CHARACTERISTIC_UUID }
            ?.subscribe()
            ?.mapNotNull { DistanceMeasurementDataParser().parse(it) }
            ?.onEach { DFSRepository.addNewDistance(deviceId, it) }
            ?.catch { it.logAndReport() }
            ?.onCompletion { DFSRepository.clear(deviceId) }
            ?.launchIn(scope)

        remoteService.characteristics
            .firstOrNull { it.uuid == ELEVATION_MEASUREMENT_CHARACTERISTIC_UUID }
            ?.subscribe()
            ?.mapNotNull { ElevationMeasurementDataParser().parse(it) }
            ?.onEach { DFSRepository.addNewElevation(deviceId, it) }
            ?.catch { it.logAndReport() }
            ?.onCompletion { DFSRepository.clear(deviceId) }
            ?.launchIn(scope)

        val ddfFeatureCharacteristics = remoteService.characteristics
            .firstOrNull { it.uuid == DDF_FEATURE_CHARACTERISTIC_UUID }
            ?.apply { ddfFeatureCharacteristic = this }
        val isReadPropertyAvailable = ddfFeatureCharacteristics
            ?.properties?.contains(CharacteristicProperty.READ)
        if (isReadPropertyAvailable == true) {
            ddfFeatureCharacteristics.read()
                .let { DDFDataParser().parse(it) }
                ?.apply { DFSRepository.setAvailableDistanceModes(deviceId, this) }
        } else {
            Timber.e("Characteristic Property READ is not available for $ddfFeatureCharacteristics")
        }

        remoteService.characteristics
            .firstOrNull { it.uuid == CONTROL_POINT_CHARACTERISTIC_UUID }
            ?.apply { controlPointCharacteristic = this }
            ?.subscribe()
            ?.mapNotNull { ControlPointDataParser().parse(it) }
            ?.onEach { DFSRepository.onControlPointDataReceived(deviceId, it, scope) }
            ?.catch { it.logAndReport() }
            ?.onCompletion { DFSRepository.clear(deviceId) }
            ?.launchIn(scope)
    }

    companion object {
        private lateinit var controlPointCharacteristic: RemoteCharacteristic
        private lateinit var ddfFeatureCharacteristic: RemoteCharacteristic

        private val MCPD_ENABLED_BYTES = byteArrayOf(0x01, 0x01)
        private val RTT_ENABLED_BYTES = byteArrayOf(0x01, 0x00)
        private val CHECK_CONFIG_BYTES = byteArrayOf(0x0A)

        suspend fun enableDistanceMode(deviceId: String, mode: ControlPointMode) {
            val data = when (mode) {
                ControlPointMode.MCPD -> MCPD_ENABLED_BYTES
                ControlPointMode.RTT -> RTT_ENABLED_BYTES
            }
            try {
                controlPointCharacteristic.write(data, WriteType.WITH_RESPONSE)
                DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to enable distance mode: $mode for device: $deviceId")
                DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.FAILED)
            }

        }

        suspend fun checkForCurrentDistanceMode(deviceId: String) {
            try {
                controlPointCharacteristic.write(CHECK_CONFIG_BYTES, WriteType.WITH_RESPONSE)
                DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to check current distance mode for device: $deviceId")
                DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.FAILED)
            }
        }

        suspend fun checkAvailableFeatures(deviceId: String) {
            DFSRepository.updateNewRequestStatus(deviceId, RequestStatus.PENDING)
            val isReadPropertyAvailable = ddfFeatureCharacteristic
                .properties.contains(CharacteristicProperty.READ)
            if (isReadPropertyAvailable) {
                ddfFeatureCharacteristic.read()
                    .let { DDFDataParser().parse(it) }
                    ?.apply {
                        DFSRepository.setAvailableDistanceModes(deviceId, this)
                    }
            } else {
                Timber.e("Characteristic Property READ is not available for $ddfFeatureCharacteristic")
            }
        }
    }

}
