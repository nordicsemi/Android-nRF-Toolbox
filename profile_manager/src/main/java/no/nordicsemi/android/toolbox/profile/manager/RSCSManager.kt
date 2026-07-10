package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.RSCS_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.RSCSRepository
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSDataParser
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSFeatureDataParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val RSC_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("00002A53-0000-1000-8000-00805F9B34FB")
private val RSC_FEATURE_CHARACTERISTIC_UUID = Uuid.parse("00002A54-0000-1000-8000-00805F9B34FB")

class RSCSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(RSCS_SERVICE_UUID, deviceId, "RSCS", onReady) {
    override val profile: ServiceType = ServiceType.RSCS
    private val tag = "RSC ($deviceId)"

    val repository = RSCSRepository()

    private lateinit var measurementCharacteristic: RemoteCharacteristic
    private var featureCharacteristic: RemoteCharacteristic? = null

    override fun prepare(service: RemoteService) {
        measurementCharacteristic = service.characteristics.first { it.uuid == RSC_MEASUREMENT_CHARACTERISTIC_UUID }
        require(measurementCharacteristic.isSubscribable()) { "RSC measurement characteristic must have NOTIFY or INDICATE" }
        featureCharacteristic = service.characteristics.firstOrNull { it.uuid == RSC_FEATURE_CHARACTERISTIC_UUID }
    }

    override suspend fun CoroutineScope.initialize() {
        measurementCharacteristic.subscribe()
            .mapNotNull { RSCSDataParser.parse(it) }
            .onEach {
                Timber.tag(tag).log(Log.Level.APPLICATION, it.toString())
                repository.onRSCSDataChanged(it)
            }
            .catch { Timber.tag(tag).e(it) }
            .onCompletion { repository.clear() }
            .launchIn(this)

        featureCharacteristic?.let { char ->
            launch {
                try {
                    Timber.tag(tag).v("Reading RSC feature...")
                    RSCSFeatureDataParser.parse(char.read())?.also {
                        Timber.tag(tag).log(Log.Level.APPLICATION, "Features: $it")
                        repository.updateRSCSFeatureData(it)
                    }
                } catch (e: Exception) {
                    Timber.tag(tag).e(e, "Error reading RSC feature")
                }
            }
        }

        onReady(this@RSCSManager)
    }
}
