package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.LBS_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.LBSRepository
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val BLINKY_BUTTON_CHARACTERISTIC_UUID = Uuid.parse("00001524-1212-EFDE-1523-785FEABCD123")
private val BLINKY_LED_CHARACTERISTIC_UUID = Uuid.parse("00001525-1212-EFDE-1523-785FEABCD123")

class LBSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(LBS_SERVICE_UUID, deviceId, "LBS", onReady) {
    override val profile: ServiceType = ServiceType.LBS

    val repository = LBSRepository()

    private lateinit var ledCharacteristic: RemoteCharacteristic
    private lateinit var buttonCharacteristic: RemoteCharacteristic

    override fun prepare(service: RemoteService) {
        ledCharacteristic = service.characteristics.first { it.uuid == BLINKY_LED_CHARACTERISTIC_UUID }
        buttonCharacteristic = service.characteristics.first { it.uuid == BLINKY_BUTTON_CHARACTERISTIC_UUID }
        require(ledCharacteristic.isWritable()) { "LED characteristic must be writable" }
        require(buttonCharacteristic.isSubscribable()) { "Button characteristic must have NOTIFY or INDICATE" }
    }

    override suspend fun CoroutineScope.initialize() {
        buttonCharacteristic
            .subscribe {
                Timber.tag("LBS").log(Log.Level.APPLICATION, "Button notifications enabled")
            }
            .onStart {
                Timber.tag("LBS").v("Enabling Button notifications...")
            }
            .map { ButtonStateParser.parse(it) }
            .onEach {
                Timber.tag("LBS").log(Log.Level.APPLICATION, "Button ${if (it) "pressed" else "released"}")
                repository.updateButtonState(it)
            }
            .catch { Timber.tag("LBS").e(it) }
            .onCompletion { repository.clear() }
            .launchIn(this)

        if (buttonCharacteristic.isReadable()) {
            try {
                Timber.tag("LBS").v("Reading initial button state...")
                val state = ButtonStateParser.parse(buttonCharacteristic.read())
                Timber.tag("LBS")
                    .log(Log.Level.APPLICATION, "Button ${if (state) "pressed" else "released"}")
                repository.updateButtonState(state)
            } catch (e: Exception) {
                Timber.tag("LBS").e(e, "Error reading initial button state")
            }
        }

        onReady(this@LBSManager)
    }

    suspend fun writeLED(ledState: Boolean) {
        try {
            Timber.tag("LBS").v("Turning LED ${if (ledState) "ON" else "OFF"}...")
            ledCharacteristic.write(ledState.encode())
            Timber.tag("LBS").log(Log.Level.APPLICATION, "LED ${if (ledState) "ON" else "OFF"}")
        } catch (e: Exception) {
            Timber.tag("LBS").e(e, "Error writing to LED characteristic")
        } finally {
            repository.updateLedState(ledState)
        }
    }

    private fun Boolean.encode(): ByteArray = byteArrayOf((if (this) 0x01 else 0x00).toByte())
}

object ButtonStateParser {
    fun parse(data: ByteArray): Boolean = data.isNotEmpty() && data[0].toInt() == 0x01
}
