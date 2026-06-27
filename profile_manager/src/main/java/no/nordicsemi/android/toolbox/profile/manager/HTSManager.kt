package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.manager.repository.HTSRepository
import no.nordicsemi.android.toolbox.profile.parser.hts.HTSDataParser
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid

private val HTS_MEASUREMENT_CHARACTERISTIC_UUID =
    Uuid.parse("00002A1C-0000-1000-8000-00805f9b34fb")

internal class HTSManager : ServiceManager {
    override val profile: Profile = Profile.HTS

    override suspend fun observeServiceInteractions(
        deviceId: String,
        remoteService: RemoteService,
        scope: CoroutineScope
    ) {
        remoteService.characteristics.firstOrNull { it.uuid == HTS_MEASUREMENT_CHARACTERISTIC_UUID }
            ?.subscribe()
            ?.mapNotNull { HTSDataParser.parse(it) }
            ?.onEach { htsData ->
                HTSRepository.updateHTSData(deviceId, htsData)
            }
            ?.onCompletion { HTSRepository.clear(deviceId) }
            ?.catch { e ->
                Timber.e(e)
            }
            ?.launchIn(scope)
    }
}