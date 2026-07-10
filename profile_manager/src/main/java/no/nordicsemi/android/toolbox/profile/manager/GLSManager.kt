package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.GLS_SERVICE_UUID
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
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val GLUCOSE_MEASUREMENT_CHARACTERISTIC = Uuid.parse("00002A18-0000-1000-8000-00805f9b34fb")
private val GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC = Uuid.parse("00002A34-0000-1000-8000-00805f9b34fb")
private val GLUCOSE_FEATURE_CHARACTERISTIC = Uuid.parse("00002A51-0000-1000-8000-00805f9b34fb")
private val RACP_CHARACTERISTIC = Uuid.parse("00002A52-0000-1000-8000-00805f9b34fb")

class GLSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(GLS_SERVICE_UUID, deviceId, "GLS", onReady) {
    override val profile: ServiceType = ServiceType.GLS
    private val tag = "GLS ($deviceId)"

    val repository = GLSRepository()

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
            .onEach {
                Timber.tag(tag).log(Log.Level.APPLICATION, it.toString())
                repository.updateNewRecord(it)
            }
            .onCompletion { repository.clear() }
            .catch { Timber.tag(tag).e(it) }
            .launchIn(this)

        contextCharacteristic?.subscribe()
            ?.mapNotNull { GlucoseMeasurementContextParser.parse(it) }
            ?.onEach {
                Timber.tag(tag).log(Log.Level.APPLICATION, it.toString())
                repository.updateWithNewContext(it)
            }
            ?.onCompletion { repository.clear() }
            ?.catch { Timber.tag(tag).e(it) }
            ?.launchIn(this)

        racpCharacteristic.subscribe()
            .mapNotNull { RecordAccessControlPointParser.parse(it) }
            .onEach {
                Timber.tag(tag).log(Log.Level.APPLICATION, it.toString())
                onAccessControlPointDataReceived(it, this)
            }
            .catch { Timber.tag(tag).e(it) }
            .launchIn(this)

        onReady(this@GLSManager)
    }

    private fun onAccessControlPointDataReceived(
        data: RecordAccessControlPointData,
        scope: CoroutineScope,
    ) {
        scope.launch {
            when (data) {
                is NumberOfRecordsData -> onNumberOfRecordsReceived(data.numberOfRecords)
                is ResponseData -> when (data.responseCode) {
                    RACPResponseCode.RACP_RESPONSE_SUCCESS ->
                        onRecordAccessOperationCompleted(data.requestCode)
                    RACPResponseCode.RACP_ERROR_OP_CODE_NOT_SUPPORTED ->
                        onRecordAccessOperationCompletedWithNoRecordsFound()
                    else -> onRecordAccessOperationError(data.responseCode)
                }
            }
        }
    }

    private fun onRecordAccessOperationError(responseCode: RACPResponseCode) {
        repository.updateNewRequestStatus(
            if (responseCode == RACPResponseCode.RACP_ERROR_OP_CODE_NOT_SUPPORTED)
                RequestStatus.NOT_SUPPORTED
            else
                RequestStatus.FAILED,
        )
    }

    private fun onRecordAccessOperationCompleted(requestCode: RACPOpCode) {
        repository.updateNewRequestStatus(
            if (requestCode == RACPOpCode.RACP_OP_CODE_ABORT_OPERATION)
                RequestStatus.ABORTED
            else
                RequestStatus.SUCCESS,
        )
    }

    private fun onRecordAccessOperationCompletedWithNoRecordsFound() {
        repository.updateNewRequestStatus(RequestStatus.SUCCESS)
    }

    private suspend fun onNumberOfRecordsReceived(numberOfRecords: Int) {
        val state = repository.data
        val highestSequenceNumber = state.value.records.keys
            .maxByOrNull { it.sequenceNumber }?.sequenceNumber ?: -1

        if (numberOfRecords > 0) {
            try {
                if (state.value.records.isNotEmpty()) {
                    Timber.tag(tag)
                        .v("Requesting records greater or equal to: $highestSequenceNumber...")
                    racpCharacteristic.write(
                        RecordAccessControlPointInputParser.reportStoredRecordsGreaterThenOrEqualTo(
                            highestSequenceNumber.toShort()
                        ),
                    )
                } else {
                    Timber.tag(tag).v("Requesting ${WorkingMode.ALL}...")
                    racpCharacteristic.write(
                        RecordAccessControlPointInputParser.reportAllStoredRecords(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(tag).e("Request failed: ${e.message}")
                repository.updateNewRequestStatus(RequestStatus.FAILED)
                return
            }
        }
        repository.updateNewRequestStatus(RequestStatus.SUCCESS)
    }

    suspend fun requestRecord(workingMode: WorkingMode) {
        repository.clearState()
        repository.updateNewRequestStatus(RequestStatus.PENDING)
        repository.updateWorkingMode(workingMode)
        try {
            if (workingMode == WorkingMode.ALL) {
                Timber.tag(tag).v("Requesting number of stored records...")
            } else {
                Timber.tag(tag).v("Requesting $workingMode...")
            }
            racpCharacteristic.write(
                when (workingMode) {
                    WorkingMode.ALL -> RecordAccessControlPointInputParser.reportNumberOfAllStoredRecords()
                    WorkingMode.LAST -> RecordAccessControlPointInputParser.reportLastStoredRecord()
                    WorkingMode.FIRST -> RecordAccessControlPointInputParser.reportFirstStoredRecord()
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(tag).e("Request failed: ${e.message}")
            repository.updateNewRequestStatus(RequestStatus.FAILED)
        }
    }
}
