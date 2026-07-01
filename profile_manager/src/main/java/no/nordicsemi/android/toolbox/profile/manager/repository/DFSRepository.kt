package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.profile.data.DFSServiceData
import no.nordicsemi.android.toolbox.profile.data.SensorData
import no.nordicsemi.android.toolbox.profile.data.SensorValue
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.azimuthal.AzimuthMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointChangeModeError
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointChangeModeSuccess
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointCheckModeError
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointCheckModeSuccess
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint.ControlPointResult
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.ddf.DDFData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.DistanceMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.McpdMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.distance.RttMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.elevation.ElevationMeasurementData
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus

class DFSRepository {
    private val _data = MutableStateFlow(DFSServiceData())
    val data: StateFlow<DFSServiceData> = _data.asStateFlow()

    fun updateSelectedDevice(device: PeripheralBluetoothAddress) {
        _data.update { it.copy(selectedDevice = device) }
    }

    fun addNewAzimuth(azimuth: AzimuthMeasurementData) {
        _data.update { current ->
            val validatedAzimuth = azimuth.copy(azimuth = azimuth.azimuth.coerceIn(0, 359))
            val key = validatedAzimuth.address
            val sensorData = current.data[key] ?: SensorData()
            val azimuths = sensorData.azimuth ?: SensorValue()
            val newSensorData = sensorData.copy(azimuth = azimuths.copyWithNewValue(validatedAzimuth))
            current.copy(data = current.data.toMutableMap().apply { put(key, newSensorData) }.toMap())
        }
    }

    fun addNewDistance(distance: DistanceMeasurementData) {
        when (distance) {
            is McpdMeasurementData -> addDistance(distance)
            is RttMeasurementData -> addDistance(distance)
        }
    }

    private fun addDistance(distance: DistanceMeasurementData) {
        _data.update { current ->
            val key = distance.address
            val sensorData = current.data[key] ?: SensorData()
            val newSensorData = when (distance) {
                is McpdMeasurementData -> sensorData.copy(
                    mcpdDistance = sensorData.mcpdDistance?.copyWithNewValue(distance) ?: SensorValue(),
                )
                is RttMeasurementData -> sensorData.copy(
                    rttDistance = sensorData.rttDistance?.copyWithNewValue(distance) ?: SensorValue(),
                )
            }
            current.copy(data = current.data.toMutableMap().apply { put(key, newSensorData) }.toMap())
        }
    }

    fun addNewElevation(elevation: ElevationMeasurementData) {
        _data.update { current ->
            val validatedElevation = elevation.copy(elevation = elevation.elevation.coerceIn(-90, 90))
            val key = validatedElevation.address
            val sensorData = current.data[key] ?: SensorData()
            val elevations = sensorData.elevation ?: SensorValue()
            val newSensorData = sensorData.copy(elevation = elevations.copyWithNewValue(validatedElevation))
            current.copy(data = current.data.toMutableMap().apply { put(key, newSensorData) }.toMap())
        }
    }

    fun updateNewRequestStatus(requestStatus: RequestStatus) {
        _data.update { it.copy(requestStatus = requestStatus) }
    }

    fun setAvailableDistanceModes(ddfData: DDFData) {
        updateNewRequestStatus(RequestStatus.PENDING)
        _data.update {
            it.copy(
                ddfFeature = DDFData(
                    isMcpdAvailable = ddfData.isMcpdAvailable,
                    isRttAvailable = ddfData.isRttAvailable,
                )
            )
        }
        updateNewRequestStatus(RequestStatus.SUCCESS)
    }

    fun onControlPointDataReceived(
        data: ControlPointResult,
        scope: CoroutineScope,
        onCheckMode: suspend () -> Unit,
    ) {
        when (data) {
            ControlPointChangeModeError, ControlPointCheckModeError -> {
                scope.launch {
                    updateNewRequestStatus(RequestStatus.PENDING)
                    onCheckMode()
                    updateNewRequestStatus(RequestStatus.FAILED)
                }
            }
            is ControlPointChangeModeSuccess, is ControlPointCheckModeSuccess -> {
                scope.launch { updateNewRequestStatus(RequestStatus.SUCCESS) }
            }
        }
    }

    fun updateDistanceRange(range: IntRange) {
        _data.update { it.copy(distanceRange = range) }
    }

    fun clear() {
        _data.value = DFSServiceData()
    }
}
