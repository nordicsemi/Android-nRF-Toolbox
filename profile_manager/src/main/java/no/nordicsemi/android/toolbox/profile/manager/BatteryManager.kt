package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.android.toolbox.lib.utils.spec.BATTERY_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.BatteryRepository
import no.nordicsemi.android.toolbox.profile.parser.battery.BatteryLevelParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty
import timber.log.Timber
import kotlin.uuid.Uuid

private val BATTERY_LEVEL_CHARACTERISTIC_UUID: Uuid = Uuid.parse("00002A19-0000-1000-8000-00805f9b34fb")

internal class BatteryManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(BATTERY_SERVICE_UUID, deviceId, "Battery", onReady) {
    override val profile: ServiceType = ServiceType.BATTERY

    private lateinit var batteryLevelCharacteristic: RemoteCharacteristic

    override fun prepare(service: RemoteService) {
        batteryLevelCharacteristic = service.characteristics.first { it.uuid == BATTERY_LEVEL_CHARACTERISTIC_UUID }
    }

    override suspend fun CoroutineScope.initialize() {
        if (batteryLevelCharacteristic.isSubscribable()) {
            batteryLevelCharacteristic.subscribe()
                .mapNotNull { BatteryLevelParser.parse(it) }
                .onEach { BatteryRepository.updateBatteryLevel(deviceId, it) }
                .onCompletion { BatteryRepository.clear(deviceId) }
                .catch { e -> Timber.e(e) }
                .launchIn(this)
        }

        if (batteryLevelCharacteristic.properties.contains(CharacteristicProperty.READ)) {
            launch {
                try {
                    BatteryLevelParser.parse(batteryLevelCharacteristic.read())
                        ?.let { BatteryRepository.updateBatteryLevel(deviceId, it) }
                } catch (e: Exception) {
                    Timber.e("Error reading battery level: ${e.message}")
                }
            }
        }

        onReady(this@BatteryManager)
    }
}
