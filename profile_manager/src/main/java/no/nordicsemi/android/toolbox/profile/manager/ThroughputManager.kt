package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CancellationException
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
import kotlin.time.measureTime
import kotlin.uuid.Uuid

private val THROUGHPUT_CHAR_UUID = Uuid.parse("00001524-0000-1000-8000-00805F9B34FB")

class ThroughputManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(THROUGHPUT_SERVICE_UUID, deviceId, "Throughput", onReady) {
    override val profile: ServiceType = ServiceType.THROUGHPUT
    private val tag = "Throughput ($deviceId)"

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
        val peripheral = writeCharacteristic.owner ?: return
        val logger = peripheral.logger

        Timber.tag(tag).log(Log.Level.APPLICATION, "Starting throughput test: $inputType...")

        val duration = measureTime {
            try {
                // Disable logging to increase throughput.
                peripheral.logger = null

                repository.updateWriteStatus(WritingStatus.IN_PROGRESS)

                resetMetrics()
                when (inputType) {
                    is NumberOfBytes -> writeBytesData(maxWriteValueLength, inputType.numberOfBytes)
                    is NumberOfSeconds -> writeTimesData(maxWriteValueLength, inputType.numberOfSeconds)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(tag).e("Test failed failed: ${e.message}")
                repository.updateWriteStatus(WritingStatus.IDLE)
                return
            } finally {
                // Re-enable the logger. Note, that the characteristic's owner may already be set
                // to null here, if the device disconnected. Hence, we grabbed the peripheral
                // when it was available.
                peripheral.logger = logger
            }
            readThroughputMetrics()
            repository.updateWriteStatus(WritingStatus.COMPLETED)
        }
        Timber.tag(tag).log(Log.Level.APPLICATION, "Test completed after: $duration")
    }

    private suspend fun resetMetrics() {
        // Write one byte to the characteristic to reset the metrics.
        writeCharacteristic.write(byteArrayOf(0x3D), WriteType.WITHOUT_RESPONSE)
    }

    private suspend fun writeBytesData(maxWriteValueLength: Int, numberOfBytes: Int) {
        // Write any data to the characteristic to measure throughput.
        val array = ByteArray(numberOfBytes) { 0x3D }
        array.chunked(maxWriteValueLength).forEach {
            writeCharacteristic.write(it, WriteType.WITHOUT_RESPONSE)
        }
    }

    // Note: This test takes longer than `numberOfSeconds`.
    //       The packets are quickly written to the outgoing queue for the given number
    //       of seconds, but then Android needs to flush the buffer, which takes longer.
    //       It was calculated, that it is enough to write for ~0.4 of time.
    //       Read response will be delayed until all writes were complete.
    private suspend fun writeTimesData(maxWriteValueLength: Int, numberOfSeconds: Int) {
        // Write any data to the characteristic to measure throughput.
        val array = ByteArray(maxWriteValueLength) { 0x3D }
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < numberOfSeconds * 400L) {
            writeCharacteristic.write(array, WriteType.WITHOUT_RESPONSE)
        }
    }

    private suspend fun readThroughputMetrics() {
        try {
            Timber.tag(tag).v("Reading throughput metrics...")
            ThroughputDataParser.parse(writeCharacteristic.read())
                ?.let {
                    Timber.tag(tag).log(Log.Level.APPLICATION, it.toString())
                    repository.updateThroughput(it)
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(tag).e("Reading throughput metrics failed: ${e.message}")
        }
    }
}
