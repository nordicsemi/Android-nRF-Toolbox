package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.android.toolbox.lib.utils.spec.UART_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.UartRepository
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.core.util.chunked
import timber.log.Timber
import kotlin.uuid.Uuid

private val UART_RX_CHARACTERISTIC_UUID = Uuid.parse("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
private val UART_TX_CHARACTERISTIC_UUID = Uuid.parse("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

internal class UARTManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(UART_SERVICE_UUID, deviceId, "UART", onReady) {
    override val profile: ServiceType = ServiceType.UART

    private lateinit var txCharacteristic: RemoteCharacteristic
    private lateinit var rxCharacteristic: RemoteCharacteristic

    override fun prepare(service: RemoteService) {
        txCharacteristic = service.characteristics.first { it.uuid == UART_TX_CHARACTERISTIC_UUID }
        rxCharacteristic = service.characteristics.first { it.uuid == UART_RX_CHARACTERISTIC_UUID }
        require(txCharacteristic.isSubscribable()) { "UART TX characteristic must have NOTIFY or INDICATE" }
        require(rxCharacteristic.isWritable()) { "UART RX characteristic must be writable" }
    }

    override suspend fun CoroutineScope.initialize() {
        txCharacteristic.subscribe()
            .map { String(it) }
            .onEach { UartRepository.onNewMessageReceived(deviceId, it) }
            .catch { it.printStackTrace() }
            .onCompletion { UartRepository.clear(deviceId) }
            .launchIn(this)

        UartRepository.registerManager(deviceId, this@UARTManager)
        onReady(this@UARTManager)
    }

    suspend fun sendText(message: String, maxWriteLength: Int) {
        try {
            message.toByteArray().chunked(maxWriteLength).forEach { rxCharacteristic.write(it) }
        } catch (e: Exception) {
            Timber.tag("UARTManager").e("Error sending text: ${e.message}")
        } finally {
            UartRepository.onNewMessageSent(deviceId, message)
        }
    }
}
