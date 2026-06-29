package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.BATTERY_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.BatteryRepository
import no.nordicsemi.android.toolbox.profile.parser.battery.BatteryLevelParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

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
            Timber.tag("Battery Service").v("Enabling battery level notifications...")
            batteryLevelCharacteristic
                .subscribe { Timber.tag("Battery Service").v("Battery level notifications enabled") }
                .mapNotNull { BatteryLevelParser.parse(it) }
                .onEach {
                    Timber.tag("Battery Service").log(Log.Level.APPLICATION, "Battery level: $it%")
                    BatteryRepository.updateBatteryLevel(deviceId, it)
                }
                .onCompletion { BatteryRepository.clear(deviceId) }
                .catch { Timber.tag("Battery Service").e(it) }
                .launchIn(this)
        }

        if (batteryLevelCharacteristic.isReadable()) {
            launch {
                try {
                    Timber.tag("Battery Service").v("Reading battery level...")
                    BatteryLevelParser.parse(batteryLevelCharacteristic.read())?.let {
                        Timber.tag("Battery Service").log(Log.Level.APPLICATION, "Battery level: $it%")
                        BatteryRepository.updateBatteryLevel(deviceId, it)
                    }
                } catch (e: Exception) {
                    Timber.tag("Battery Service").e(e, "Error reading battery level")
                }
            }
        }

        onReady(this@BatteryManager)
    }
}
