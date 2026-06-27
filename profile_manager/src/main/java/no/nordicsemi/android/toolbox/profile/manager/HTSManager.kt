package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.android.toolbox.lib.utils.spec.HTS_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.HTSRepository
import no.nordicsemi.android.toolbox.profile.parser.hts.HTSDataParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid

private val HTS_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("00002A1C-0000-1000-8000-00805f9b34fb")

internal class HTSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(HTS_SERVICE_UUID, deviceId, "HTS", onReady) {
    override val profile: ServiceType = ServiceType.HTS

    private lateinit var measurementCharacteristic: RemoteCharacteristic

    override fun prepare(service: RemoteService) {
        measurementCharacteristic = service.characteristics.first { it.uuid == HTS_MEASUREMENT_CHARACTERISTIC_UUID }
        require(measurementCharacteristic.isSubscribable()) { "HTS measurement characteristic must have NOTIFY or INDICATE" }
    }

    override suspend fun CoroutineScope.initialize() {
        measurementCharacteristic.subscribe()
            .mapNotNull { HTSDataParser.parse(it) }
            .onEach { HTSRepository.updateHTSData(deviceId, it) }
            .onCompletion { HTSRepository.clear(deviceId) }
            .catch { e -> Timber.e(e) }
            .launchIn(this)

        onReady(this@HTSManager)
    }
}
