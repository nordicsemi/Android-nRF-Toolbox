package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.BPS_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.BPSRepository
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureFeatureParser
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureMeasurementParser
import no.nordicsemi.android.toolbox.profile.parser.bps.IntermediateCuffPressureParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val BPM_CHARACTERISTIC_UUID = Uuid.parse("00002A35-0000-1000-8000-00805f9b34fb")
private val ICP_CHARACTERISTIC_UUID = Uuid.parse("00002A36-0000-1000-8000-00805f9b34fb")
private val BPF_CHARACTERISTIC_UUID = Uuid.parse("00002A49-0000-1000-8000-00805f9b34fb")

internal class BPSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(BPS_SERVICE_UUID, deviceId, "BPS", onReady) {
    override val profile: ServiceType = ServiceType.BPS

    private lateinit var bpmCharacteristic: RemoteCharacteristic
    private var icpCharacteristic: RemoteCharacteristic? = null
    private var bpfCharacteristic: RemoteCharacteristic? = null

    override fun prepare(service: RemoteService) {
        bpmCharacteristic = service.characteristics.first { it.uuid == BPM_CHARACTERISTIC_UUID }
        require(bpmCharacteristic.isSubscribable()) { "Blood Pressure measurement characteristic must have INDICATE property" }
        icpCharacteristic = service.characteristics.firstOrNull { it.uuid == ICP_CHARACTERISTIC_UUID }
        icpCharacteristic?.let {
            require(it.isSubscribable()) { "Intermediate Cuff Pressure characteristic must have NOTIFY property" }
        }
        bpfCharacteristic = service.characteristics.firstOrNull { it.uuid == BPF_CHARACTERISTIC_UUID }
        bpfCharacteristic?.let {
            require(it.isReadable()) { "Blood Pressure feature characteristic must be readable" }
        }
    }

    override suspend fun CoroutineScope.initialize() {
        bpmCharacteristic.subscribe()
            .mapNotNull { BloodPressureMeasurementParser.parse(it) }
            .onEach {
                Timber.tag("BPS").log(Log.Level.APPLICATION, it.toString())
                BPSRepository.updateBPSData(deviceId, it)
            }
            .onCompletion { BPSRepository.clear(deviceId) }
            .catch { Timber.tag("BPS").e(it) }
            .launchIn(this)

        icpCharacteristic?.subscribe()
            ?.mapNotNull { IntermediateCuffPressureParser.parse(it) }
            ?.onEach {
                Timber.tag("BPS").log(Log.Level.APPLICATION, it.toString())
                BPSRepository.updateICPData(deviceId, it)
            }
            ?.onCompletion { BPSRepository.clear(deviceId) }
            ?.catch { Timber.tag("BPS").e(it) }
            ?.launchIn(this)

        bpfCharacteristic?.let { char ->
            launch {
                try {
                    BloodPressureFeatureParser.parse(char.read())?.let {
                        Timber.tag("BPS").log(Log.Level.APPLICATION, "Features: $it")
                        BPSRepository.updateBPSFeatureData(deviceId, it)
                    }
                } catch (e: Exception) {
                    Timber.tag("BPS").e(e, "Error reading blood pressure feature")
                }
            }
        }

        onReady(this@BPSManager)
    }
}
