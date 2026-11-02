package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.LBS_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.LBSRepository
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val BLINKY_BUTTON_CHARACTERISTIC_UUID = Uuid.parse("00001524-1212-EFDE-1523-785FEABCD123")
private val BLINKY_LED_CHARACTERISTIC_UUID = Uuid.parse("00001525-1212-EFDE-1523-785FEABCD123")

class LBSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : SimpleServiceManager(LBS_SERVICE_UUID, deviceId, "LBS", onReady) {
    override val profile: ServiceType = ServiceType.LBS
    private val tag = "LBS ($deviceId)"

    val repository = LBSRepository()

    /**
     * The state of the Button -> LED binding.
     *
     * With the binding enabled, the LED state follows the button state.
     */
    val bindingState = MutableStateFlow(false)

    /** The state of the button: `true` when pressed, `false` when released. */
    private val button: Flow<Boolean> = repository.data
        .map { it.data.buttonState }
        .distinctUntilChanged()

    /** The flow of button click events. A click is a press shorter than [LONG_PRESS_TIMEOUT]. */
    val buttonPressed: Flow<Unit> = flow {
        // Flag set when a long press is detected.
        var isLongPress = false

        // Drop the initial value, it is the current state of the button.
        button.drop(1).collect { pressed ->
            if (pressed) {
                try {
                    withTimeout(LONG_PRESS_TIMEOUT) {
                        // Await the button to be released before the timeout.
                        button.drop(1).first { !it }
                    }
                } catch (_: TimeoutCancellationException) {
                    // The button has not been released before the time ran out.
                    isLongPress = true
                }
            } else {
                // If released, emit the event only if it was not a long press.
                if (!isLongPress) {
                    emit(Unit)
                }
                isLongPress = false
            }
        }
    }

    /** The flow of long click events, emitted when the button is pressed for [LONG_PRESS_TIMEOUT]. */
    val buttonLongPressed: Flow<Unit> = button
        .flatMapLatest { pressed ->
            if (pressed) flow {
                delay(LONG_PRESS_TIMEOUT)
                emit(Unit)
            } else emptyFlow()
        }

    private lateinit var ledCharacteristic: RemoteCharacteristic
    private lateinit var buttonCharacteristic: RemoteCharacteristic

    override fun prepare(peripheral: Peripheral<*, *>, service: RemoteService) {
        ledCharacteristic = service.characteristics.first { it.uuid == BLINKY_LED_CHARACTERISTIC_UUID }
        buttonCharacteristic = service.characteristics.first { it.uuid == BLINKY_BUTTON_CHARACTERISTIC_UUID }
        require(ledCharacteristic.isWritable()) { "LED characteristic must be writable" }
        require(buttonCharacteristic.isSubscribable()) { "Button characteristic must have NOTIFY or INDICATE" }
    }

    override suspend fun CoroutineScope.initialize(peripheral: Peripheral<*, *>) {
        buttonCharacteristic
            .subscribe {
                Timber.tag(tag).log(Log.Level.APPLICATION, "Button notifications enabled")
            }
            .onStart {
                Timber.tag(tag).v("Enabling Button notifications...")
            }
            .map { ButtonStateParser.parse(it) }
            .onEach {
                Timber.tag(tag).log(Log.Level.APPLICATION, "Button ${if (it) "pressed" else "released"}")
                repository.updateButtonState(it)
            }
            .catch { Timber.tag(tag).e(it) }
            .onCompletion { repository.clear() }
            .launchIn(this)

        // When the binding is enabled, the LED follows the button, starting from its current state.
        bindingState
            .flatMapLatest { bound -> if (bound) button else emptyFlow() }
            .onEach { writeLED(it) }
            .catch { Timber.tag(tag).e(it) }
            .launchIn(this)

        onReady(this@LBSManager)

        if (buttonCharacteristic.isReadable()) {
            try {
                Timber.tag(tag).v("Reading initial button state...")
                val state = ButtonStateParser.parse(buttonCharacteristic.read())
                Timber.tag(tag).log(Log.Level.APPLICATION, "Button ${if (state) "pressed" else "released"}")
                repository.updateButtonState(state)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(tag).e("Error reading initial button state: ${e.message}")
            }
        }
    }

    suspend fun writeLED(ledState: Boolean) {
        try {
            Timber.tag(tag).v("Turning LED ${if (ledState) "ON" else "OFF"}...")
            ledCharacteristic.write(ledState.encode())
            Timber.tag(tag).log(Log.Level.APPLICATION, "LED ${if (ledState) "ON" else "OFF"}")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error writing to LED characteristic")
        } finally {
            repository.updateLedState(ledState)
        }
    }

    /** Toggles the LED [count] times, with 0.5 s between the changes. */
    suspend fun blink(count: Int) {
        repeat(count * 2) {
            writeLED(!repository.data.value.data.ledState)
            delay(500)
        }
    }

    companion object {
        /** If the button is pressed for at least this time, it is reported as a long press. */
        val LONG_PRESS_TIMEOUT = 1.seconds
    }

    private fun Boolean.encode(): ByteArray = byteArrayOf((if (this) 0x01 else 0x00).toByte())
}

object ButtonStateParser {
    fun parse(data: ByteArray): Boolean = data.isNotEmpty() && data[0].toInt() == 0x01
}
