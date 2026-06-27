package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.android.toolbox.lib.utils.logAndReport
import no.nordicsemi.android.toolbox.lib.utils.spec.GLS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.tryOrLog
import no.nordicsemi.android.toolbox.profile.manager.repository.GLSRepository
import no.nordicsemi.android.toolbox.profile.parser.common.WorkingMode
import no.nordicsemi.android.toolbox.profile.parser.gls.GlucoseMeasurementContextParser
import no.nordicsemi.android.toolbox.profile.parser.gls.GlucoseMeasurementParser
import no.nordicsemi.android.toolbox.profile.parser.gls.RecordAccessControlPointInputParser
import no.nordicsemi.android.toolbox.profile.parser.gls.RecordAccessControlPointParser
import no.nordicsemi.android.toolbox.profile.parser.gls.data.NumberOfRecordsData
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RecordAccessControlPointData
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus
import no.nordicsemi.android.toolbox.profile.parser.gls.data.ResponseData
import no.nordicsemi.android.toolbox.profile.parser.racp.RACPOpCode
import no.nordicsemi.android.toolbox.profile.parser.racp.RACPResponseCode
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.core.WriteType
import kotlin.uuid.Uuid

private val GLUCOSE_MEASUREMENT_CHARACTERISTIC = Uuid.parse("00002A18-0000-1000-8000-00805f9b34fb")
private val GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC = Uuid.parse("00002A34-0000-1000-8000-00805f9b34fb")
private val GLUCOSE_FEATURE_CHARACTERISTIC = Uuid.parse("00002A51-0000-1000-8000-00805f9b34fb")
private val RACP_CHARACTERISTIC = Uuid.parse("00002A52-0000-1000-8000-00805f9b34fb")

internal class GLSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(GLS_SERVICE_UUID, deviceId, "GLS", onReady) {
    override val profile: ServiceType = ServiceType.GLS

    private lateinit var glucoseMeasurementCharacteristic: RemoteCharacteristic
    private lateinit var racpCharacteristic: RemoteCharacteristic
    private var contextCharacteristic: RemoteCharacteristic? = null

    override fun prepare(service: RemoteService) {
        glucoseMeasurementCharacteristic = service.characteristics.first { it.uuid == GLUCOSE_MEASUREMENT_CHARACTERISTIC }
        require(glucoseMeasurementCharacteristic.isSubscribable()) { "Glucose measurement characteristic must have NOTIFY or INDICATE" }
        racpCharacteristic = service.characteristics.first { it.uuid == RACP_CHARACTERISTIC }
        require(racpCharacteristic.isWritable()) { "RACP characteristic must be writable" }
        require(racpCharacteristic.isSubscribable()) { "RACP characteristic must be subscribable" }
        contextCharacteristic = service.characteristics.firstOrNull { it.uuid == GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC }
    }

    override suspend fun CoroutineScope.initialize() {
        glucoseMeasurementCharacteristic.subscribe()
            .mapNotNull { GlucoseMeasurementParser.parse(it) }
            .onEach { GLSRepository.updateNewRecord(deviceId, it) }
            .onCompletion { GLSRepository.clear(deviceId) }
            .catch { it.logAndReport() }
            .launchIn(this)

        contextCharacteristic?.subscribe()
            ?.mapNotNull { GlucoseMeasurementContextParser.parse(it) }
            ?.onEach { GLSRepository.updateWithNewContext(deviceId, it) }
            ?.onCompletion { GLSRepository.clear(deviceId) }
            ?.catch { it.logAndReport() }
            ?.launchIn(this)

        racpCharacteristic.subscribe()
            .mapNotNull { RecordAccessControlPointParser.parse(it) }
            .onEach { onAccessControlPointDataReceived(deviceId, it, this) }
            .catch { it.logAndReport() }
            .launchIn(this)

        GLSRepository.registerManager(deviceId, this@GLSManager)
        onReady(this@GLSManager)
    }

    private fun onAccessControlPointDataReceived(
        deviceId: String,
        data: RecordAccessControlPointData,
        scope: CoroutineScope,
    ) {
        scope.launch {
            when (data) {
                is NumberOfRecordsData -> onNumberOfRecordsReceived(deviceId, data.numberOfRecords)
                is ResponseData -> when (data.responseCode) {
                    RACPResponseCode.RACP_RESPONSE_SUCCESS ->
                        onRecordAccessOperationCompleted(deviceId, data.requestCode)
                    RACPResponseCode.RACP_ERROR_OP_CODE_NOT_SUPPORTED ->
                        onRecordAccessOperationCompletedWithNoRecordsFound(deviceId)
                    else -> onRecordAccessOperationError(deviceId, data.responseCode)
                }
            }
        }
    }

    private fun onRecordAccessOperationError(deviceId: String, responseCode: RACPResponseCode) {
        GLSRepository.updateNewRequestStatus(
            deviceId,
            if (responseCode == RACPResponseCode.RACP_ERROR_OP_CODE_NOT_SUPPORTED)
                RequestStatus.NOT_SUPPORTED
            else
                RequestStatus.FAILED,
        )
    }

    private fun onRecordAccessOperationCompleted(deviceId: String, requestCode: RACPOpCode) {
        GLSRepository.updateNewRequestStatus(
            deviceId,
            if (requestCode == RACPOpCode.RACP_OP_CODE_ABORT_OPERATION)
                RequestStatus.ABORTED
            else
                RequestStatus.SUCCESS,
        )
    }

    private fun onRecordAccessOperationCompletedWithNoRecordsFound(deviceId: String) {
        GLSRepository.updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
    }

    private suspend fun onNumberOfRecordsReceived(deviceId: String, numberOfRecords: Int) {
        val state = GLSRepository.getData(deviceId)
        val highestSequenceNumber = state.value.records.keys
            .maxByOrNull { it.sequenceNumber }?.sequenceNumber ?: -1

        if (numberOfRecords > 0) {
            tryOrLog {
                racpCharacteristic.write(
                    if (state.value.records.isNotEmpty())
                        RecordAccessControlPointInputParser.reportStoredRecordsGreaterThenOrEqualTo(
                            highestSequenceNumber.toShort()
                        )
                    else
                        RecordAccessControlPointInputParser.reportAllStoredRecords(),
                    WriteType.WITH_RESPONSE,
                )
            }
        }
        GLSRepository.updateNewRequestStatus(deviceId, RequestStatus.SUCCESS)
    }

    suspend fun requestRecord(workingMode: WorkingMode) {
        try {
            racpCharacteristic.write(
                when (workingMode) {
                    WorkingMode.ALL -> RecordAccessControlPointInputParser.reportNumberOfAllStoredRecords()
                    WorkingMode.LAST -> RecordAccessControlPointInputParser.reportLastStoredRecord()
                    WorkingMode.FIRST -> RecordAccessControlPointInputParser.reportFirstStoredRecord()
                },
                WriteType.WITH_RESPONSE,
            )
        } catch (e: Exception) {
            GLSRepository.updateNewRequestStatus(deviceId, RequestStatus.FAILED)
        }
    }
}
