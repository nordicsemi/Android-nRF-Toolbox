package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.android.toolbox.lib.utils.spec.THROUGHPUT_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.data.NumberOfBytes
import no.nordicsemi.android.toolbox.profile.data.NumberOfSeconds
import no.nordicsemi.android.toolbox.profile.data.ThroughputInputType
import no.nordicsemi.android.toolbox.profile.data.WritingStatus
import no.nordicsemi.android.toolbox.profile.manager.repository.ThroughputRepository
import no.nordicsemi.android.toolbox.profile.parser.throughput.ThroughputDataParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.ble.core.util.chunked
import timber.log.Timber
import kotlin.uuid.Uuid

private val THROUGHPUT_CHAR_UUID = Uuid.parse("00001524-0000-1000-8000-00805F9B34FB")

class ThroughputManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(THROUGHPUT_SERVICE_UUID, deviceId, "Throughput", onReady) {
    override val profile: ServiceType = ServiceType.THROUGHPUT

    val repository = ThroughputRepository()

    private lateinit var writeCharacteristic: RemoteCharacteristic

    override fun prepare(service: RemoteService) {
        writeCharacteristic = service.characteristics.first { it.uuid == THROUGHPUT_CHAR_UUID }
        require(writeCharacteristic.isWritable()) { "Throughput characteristic must be writable" }
    }

    override suspend fun CoroutineScope.initialize() {
        repository.clearData()
        onReady(this@ThroughputManager)
    }

    suspend fun writeRequest(maxWriteValueLength: Int, inputType: ThroughputInputType) {
        try {
            repository.updateWriteStatus(WritingStatus.IN_PROGRESS)
            when (inputType) {
                is NumberOfBytes -> writeBytesData(maxWriteValueLength, inputType.numberOfBytes)
                is NumberOfSeconds -> writeTimesData(maxWriteValueLength, inputType.numberOfSeconds)
            }
        } catch (e: Exception) {
            Timber.tag("Throughput Service").e(e, "Write request failed")
        } finally {
            readThroughputMetrics()
            repository.updateWriteStatus(WritingStatus.COMPLETED)
        }
    }

    private suspend fun writeBytesData(maxWriteValueLength: Int, numberOfBytes: Int) {
        val array = ByteArray(numberOfBytes) { 0x3D }
        writeCharacteristic.write(byteArrayOf(0x3D), WriteType.WITHOUT_RESPONSE)
        array.chunked(maxWriteValueLength).forEach {
            writeCharacteristic.write(it, WriteType.WITHOUT_RESPONSE)
        }
    }

    private suspend fun writeTimesData(maxWriteValueLength: Int, numberOfSeconds: Int) {
        val array = ByteArray(maxWriteValueLength) { 0x3D }
        val startTime = System.currentTimeMillis()
        writeCharacteristic.write(byteArrayOf(0x3D), WriteType.WITHOUT_RESPONSE)
        while (System.currentTimeMillis() - startTime < numberOfSeconds * 1000L) {
            writeCharacteristic.write(array, WriteType.WITHOUT_RESPONSE)
        }
    }

    private suspend fun readThroughputMetrics() {
        try {
            Timber.tag("Throughput Service").v("Reading throughput metrics...")
            ThroughputDataParser.parse(writeCharacteristic.read())
                ?.let {
                    Timber.tag("Throughput Service").log(Log.Level.APPLICATION, it.toString())
                    repository.updateThroughput(it)
                }
        } catch (e: Exception) {
            Timber.tag("Throughput Service").e(e, "Error reading throughput metrics")
        }
    }
}
