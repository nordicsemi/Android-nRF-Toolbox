package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.data.DFSServiceData
import no.nordicsemi.android.toolbox.profile.data.SensorData
import no.nordicsemi.android.toolbox.profile.data.SensorValue
import no.nordicsemi.android.toolbox.profile.manager.DDFSManager
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.azimuthal.AzimuthMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointChangeModeError
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointChangeModeSuccess
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointCheckModeError
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointCheckModeSuccess
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointMode
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointResult
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.ddf.DDFData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.DistanceMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.McpdMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.RttMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.elevation.ElevationMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus

object DFSRepository {
    private val _dataMap = mutableMapOf<String, MutableStateFlow<DFSServiceData>>()
    private val _managers = mutableMapOf<String, DDFSManager>()

    internal fun registerManager(deviceId: String, manager: DDFSManager) {
        _managers[deviceId] = manager
    }

    fun getData(deviceId: String): StateFlow<DFSServiceData> = _dataMap.getOrPut(deviceId) {
        MutableStateFlow(DFSServiceData())
    }

    fun updateSelectedDevice(deviceId: String, device: PeripheralBluetoothAddress) {
        _dataMap[deviceId]?.update { it.copy(selectedDevice = device) }
    }

    fun addNewAzimuth(deviceId: String, azimuth: AzimuthMeasurementData) {
        _dataMap[deviceId]?.update { current ->
            val validatedAzimuth = azimuth.copy(azimuth = azimuth.azimuth.coerceIn(0, 359))
            val key = validatedAzimuth.address
            val sensorData = current.data[key] ?: SensorData()
            val azimuths = sensorData.azimuth ?: SensorValue()
            val newAzimuths = azimuths.copyWithNewValue(validatedAzimuth)
            val newSensorData = sensorData.copy(azimuth = newAzimuths)
            val newDevicesData = current.data.toMutableMap().apply {
                put(key, newSensorData)
            }.toMap()
            current.copy(data = newDevicesData)
        }
    }

    fun addNewDistance(deviceId: String, distance: DistanceMeasurementData) {
        when (distance) {
            is McpdMeasurementData -> addDistance(deviceId, distance)
            is RttMeasurementData -> addDistance(deviceId, distance)
        }
    }

    fun clear(deviceId: String) {
        _dataMap.remove(deviceId)
        _managers.remove(deviceId)
    }

    private fun addDistance(
        deviceId: String,
        distance: DistanceMeasurementData,
    ) {
        _dataMap[deviceId]?.update { current ->
            val key = distance.address
            val sensorData = current.data[key] ?: SensorData()
            val newSensorData = when (distance) {
                is McpdMeasurementData -> sensorData.copy(
                    mcpdDistance = sensorData.mcpdDistance
                        ?.copyWithNewValue(distance) ?: SensorValue(),
                )

                is RttMeasurementData -> sensorData.copy(
                    rttDistance = sensorData.rttDistance
                        ?.copyWithNewValue(distance) ?: SensorValue(),
                )
            }
            val newDevicesData = current.data.toMutableMap().apply {
                put(key, newSensorData)
            }.toMap()
            current.copy(data = newDevicesData)
        }
    }

    fun addNewElevation(deviceId: String, elevation: ElevationMeasurementData) {
        _dataMap[deviceId]?.update { current ->
            val validatedElevation =
                elevation.copy(elevation = elevation.elevation.coerceIn(-90, 90))
            val key = validatedElevation.address
            val sensorData = current.data[key] ?: SensorData()
            val elevations = sensorData.elevation ?: SensorValue()
            val newElevation = elevations.copyWithNewValue(validatedElevation)
            val newSensorData = sensorData.copy(elevation = newElevation)
            val newDevicesData = current.data
                .toMutableMap()
                .apply { put(key, newSensorData) }
                .toMap()
            current.copy(data = newDevicesData)
        }
    }

    fun updateNewRequestStatus(deviceId: String, requestStatus: RequestStatus) {
        _dataMap[deviceId]?.update { it.copy(requestStatus = requestStatus) }
    }

    suspend fun enableDistanceMode(deviceId: String, distanceMode: ControlPointMode) {
        _dataMap[deviceId]?.update { it.copy(requestStatus = RequestStatus.PENDING) }
        _managers[deviceId]?.enableDistanceMode(distanceMode)
    }

    fun setAvailableDistanceModes(deviceId: String, ddfData: DDFData) {
        updateNewRequestStatus(deviceId, RequestStatus.PENDING)
        _dataMap[deviceId]?.update {
            it.copy(
                ddfFeature = DDFData(
                    isMcpdAvailable = ddfData.isMcpdAvailable,
                    isRttAvailable = ddfData.isRttAvailable
                )
            )
        }
        updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
    }

    fun onControlPointDataReceived(
        deviceId: String,
        data: ControlPointResult,
        scope: CoroutineScope
    ) {
        when (data) {
            ControlPointChangeModeError -> {
                scope.launch {
                    checkCurrentDistanceMode(deviceId)
                    updateNewRequestStatus(deviceId, RequestStatus.FAILED)
                }
            }

            is ControlPointChangeModeSuccess -> {
                scope.launch {
                    updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
                }
            }

            ControlPointCheckModeError -> {
                scope.launch {
                    checkCurrentDistanceMode(deviceId)
                    updateNewRequestStatus(deviceId, RequestStatus.FAILED)
                }
            }

            is ControlPointCheckModeSuccess -> {
                scope.launch {
                    updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
                }
            }
        }
    }


    suspend fun checkCurrentDistanceMode(deviceId: String) {
        updateNewRequestStatus(deviceId, RequestStatus.PENDING)
        _managers[deviceId]?.checkForCurrentDistanceMode()
    }

    fun updateDistanceRange(deviceId: String, range: IntRange) {
        _dataMap[deviceId]?.update { it.copy(distanceRange = range) }
    }

    suspend fun checkAvailableFeatures(deviceId: String) {
        _managers[deviceId]?.checkAvailableFeatures()
    }

}