package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.HRS_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.HRSRepository
import no.nordicsemi.android.toolbox.profile.parser.hrs.BodySensorLocationParser
import no.nordicsemi.android.toolbox.profile.parser.hrs.HRSDataParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID = Uuid.parse("00002A38-0000-1000-8000-00805f9b34fb")
private val HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("00002A37-0000-1000-8000-00805f9b34fb")

class HRSManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(HRS_SERVICE_UUID, deviceId, "HRS", onReady) {
    override val profile: ServiceType = ServiceType.HRS
    private val tag = "HRS ($deviceId)"

    val repository = HRSRepository()

    private lateinit var hrMeasurementCharacteristic: RemoteCharacteristic
    private var bodySensorLocationCharacteristic: RemoteCharacteristic? = null

    override fun prepare(service: RemoteService) {
        hrMeasurementCharacteristic = service.characteristics.first { it.uuid == HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID }
        require(hrMeasurementCharacteristic.isSubscribable()) { "Heart Rate measurement characteristic must have NOTIFY or INDICATE" }
        bodySensorLocationCharacteristic = service.characteristics.firstOrNull { it.uuid == BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID }
    }

    override suspend fun CoroutineScope.initialize() {
        hrMeasurementCharacteristic.subscribe()
            .mapNotNull { HRSDataParser.parse(it) }
            .onEach {
                Timber.tag(tag).log(Log.Level.APPLICATION, it.toString())
                repository.updateHRSData(it)
            }
            .onCompletion { repository.clear() }
            .catch { Timber.tag(tag).e(it) }
            .launchIn(this)

        bodySensorLocationCharacteristic?.let { char ->
            launch {
                try {
                    Timber.tag(tag).v("Reading body sensor location...")
                    BodySensorLocationParser.parse(char.read())?.let {
                        Timber.tag(tag).log(Log.Level.APPLICATION, "Body sensor location: $it")
                        repository.updateBodySensorLocation(it)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(tag).e("Reading body sensor location failed: ${e.message}")
                }
            }
        }

        onReady(this@HRSManager)
    }
}
