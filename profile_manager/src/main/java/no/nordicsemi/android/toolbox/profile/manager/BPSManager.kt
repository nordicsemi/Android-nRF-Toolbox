package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureFeatureParser
import no.nordicsemi.android.toolbox.profile.parser.bps.BloodPressureMeasurementParser
import no.nordicsemi.android.toolbox.profile.parser.bps.IntermediateCuffPressureParser
import no.nordicsemi.android.toolbox.profile.manager.repository.BPSRepository
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid

private val BPM_CHARACTERISTIC_UUID = Uuid.parse("00002A35-0000-1000-8000-00805f9b34fb")
private val ICP_CHARACTERISTIC_UUID = Uuid.parse("00002A36-0000-1000-8000-00805f9b34fb")
private val BPF_CHARACTERISTIC_UUID = Uuid.parse("00002A49-0000-1000-8000-00805f9b34fb")

internal class BPSManager : ServiceManager {
    override val profile: Profile = Profile.BPS

    override suspend fun observeServiceInteractions(
        deviceId: String,
        remoteService: RemoteService,
        scope: CoroutineScope
    ) {
        remoteService.characteristics.firstOrNull { it.uuid == BPM_CHARACTERISTIC_UUID }
            ?.subscribe()
            ?.mapNotNull { BloodPressureMeasurementParser.parse(it) }
            ?.onEach { BPSRepository.updateBPSData(deviceId, it) }
            ?.onCompletion { BPSRepository.clear(deviceId) }
            ?.catch { e ->
                Timber.e(e)
            }
            ?.launchIn(scope)

        remoteService.characteristics.firstOrNull { it.uuid == ICP_CHARACTERISTIC_UUID }
            ?.subscribe()
            ?.mapNotNull { IntermediateCuffPressureParser.parse(it) }
            ?.onEach { BPSRepository.updateICPData(deviceId, it) }
            ?.onCompletion { BPSRepository.clear(deviceId) }
            ?.catch { e ->
                Timber.e(e)
            }
            ?.launchIn(scope)

        remoteService.characteristics.firstOrNull { it.uuid == BPF_CHARACTERISTIC_UUID }
            ?.read()
            ?.let { BloodPressureFeatureParser.parse(it) }
            ?.also { featureData ->
                BPSRepository.updateBPSFeatureData(deviceId, featureData)
            }
    }
}
