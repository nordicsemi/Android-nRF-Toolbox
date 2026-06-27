package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.lib.utils.logAndReport
import no.nordicsemi.android.toolbox.profile.manager.repository.RSCSRepository
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSDataParser
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSFeatureDataParser
import no.nordicsemi.kotlin.ble.client.RemoteService
import kotlin.uuid.Uuid

private val RSC_MEASUREMENT_CHARACTERISTIC_UUID = Uuid.parse("00002A53-0000-1000-8000-00805F9B34FB")
private val RSC_FEATURE_CHARACTERISTIC_UUID = Uuid.parse("00002A54-0000-1000-8000-00805F9B34FB")

internal class RSCSManager : ServiceManager {
    override val profile: Profile
        get() = Profile.RSCS

    override suspend fun observeServiceInteractions(
        deviceId: String,
        remoteService: RemoteService,
        scope: CoroutineScope
    ) {
        remoteService.characteristics
            .firstOrNull { it.uuid == RSC_MEASUREMENT_CHARACTERISTIC_UUID }
            ?.subscribe()
            ?.mapNotNull { RSCSDataParser.parse(it) }
            ?.onEach { RSCSRepository.onRSCSDataChanged(deviceId, it) }
            ?.catch { it.logAndReport() }
            ?.onCompletion { RSCSRepository.clear(deviceId) }
            ?.launchIn(scope)

        remoteService.characteristics
            .firstOrNull { it.uuid == RSC_FEATURE_CHARACTERISTIC_UUID }
            ?.read()
            ?.let {
                RSCSFeatureDataParser.parse(it)
            }
            ?.also {
                RSCSRepository.updateRSCSFeatureData(deviceId, it)
            }
    }
}
