package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.manager.repository.LBSRepository
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.core.WriteType
import timber.log.Timber
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val BLINKY_BUTTON_CHARACTERISTIC_UUID = Uuid.parse("00001524-1212-EFDE-1523-785FEABCD123")
private val BLINKY_LED_CHARACTERISTIC_UUID = Uuid.parse("00001525-1212-EFDE-1523-785FEABCD123")

internal class LBSManager : ServiceManager {
    override val profile: Profile
        get() = Profile.LBS

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun observeServiceInteractions(
        deviceId: String,
        remoteService: RemoteService,
        scope: CoroutineScope
    ) {
        // Ensure the characteristic is initialized before writing
        ledWriteCharacteristics = remoteService.characteristics.firstOrNull {
            it.uuid == BLINKY_LED_CHARACTERISTIC_UUID
        } ?: throw IllegalStateException("LED characteristic not found")

        val blinkyCharacteristics = remoteService.characteristics.firstOrNull {
            it.uuid == BLINKY_BUTTON_CHARACTERISTIC_UUID
        }

        // Subscribe to the button state changes.
        blinkyCharacteristics?.subscribe()
            ?.map { ButtonStateParser.parse(it) }
            ?.onEach { LBSRepository.updateButtonState(deviceId, it) }
            ?.catch {
                Timber.e("Error observing button state: ${it.message}")
            }
            ?.onCompletion {
                LBSRepository.clear(deviceId)
            }
            ?.launchIn(scope)

        // Read the initial state of the button
        try {
            blinkyCharacteristics?.read()
                ?.let { ButtonStateParser.parse(it) }
                ?.let { LBSRepository.updateButtonState(deviceId, it) }
        } catch (e: Exception) {
            Timber.e("Error reading button state: ${e.message}")
        }
    }

    companion object {
        private lateinit var ledWriteCharacteristics: RemoteCharacteristic

        /**
         * Writes the LED state to the Blinky LED characteristic.
         *
         * @param deviceId The ID of the device to which the LED state should be written.
         * @param ledState The desired state of the LED (true for ON, false for OFF).
         */
        suspend fun writeToBlinkyLED(
            deviceId: String,
            ledState: Boolean
        ) {
            val data = byteArrayOf((0x01.takeIf { ledState } ?: 0x00).toByte())

            try {
                if (::ledWriteCharacteristics.isInitialized) {
                    ledWriteCharacteristics.write(data, WriteType.WITHOUT_RESPONSE)
                }
            } catch (e: Exception) {
                Timber.e("Error writing to Blinky LED characteristic: ${e.message}")
            } finally {
                LBSRepository.updateLedState(deviceId, ledState)
            }
        }
    }
}

object ButtonStateParser {
    fun parse(data: ByteArray): Boolean {
        return if (data.isNotEmpty()) {
            data[0].toInt() == 0x01
        } else {
            false
        }
    }
}