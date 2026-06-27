package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.android.toolbox.lib.utils.spec.CSC_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.CSCRepository
import no.nordicsemi.android.toolbox.profile.parser.csc.CSCDataParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import kotlin.uuid.Uuid

private val CSC_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("00002A5B-0000-1000-8000-00805f9b34fb")

internal class CSCManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(CSC_SERVICE_UUID, deviceId, "CSC", onReady) {
    override val profile: ServiceType = ServiceType.CSC

    private lateinit var measurementCharacteristic: RemoteCharacteristic

    override fun prepare(service: RemoteService) {
        measurementCharacteristic = service.characteristics.first { it.uuid == CSC_MEASUREMENT_CHARACTERISTIC_UUID }
        require(measurementCharacteristic.isSubscribable()) { "CSC measurement characteristic must have NOTIFY or INDICATE" }
    }

    override suspend fun CoroutineScope.initialize() {
        measurementCharacteristic.subscribe()
            .mapNotNull {
                CSCDataParser.parse(it, CSCRepository.getData(deviceId).value.data.wheelSize)
            }
            .onEach { CSCRepository.onCSCDataChanged(deviceId, it) }
            .catch { it.printStackTrace() }
            .onCompletion { CSCRepository.clear(deviceId) }
            .launchIn(this)

        onReady(this@CSCManager)
    }
}
