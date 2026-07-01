package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import timber.log.Timber
import no.nordicsemi.android.toolbox.lib.utils.spec.DFU_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.EXPERIMENTAL_BUTTONLESS_DFU_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.LEGACY_DFU_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.MDS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.SMP_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.data.DFUsAvailable
import no.nordicsemi.android.toolbox.profile.manager.repository.DFURepository
import no.nordicsemi.kotlin.ble.client.RemoteService
import kotlin.uuid.Uuid

class DFUManager(
    private val serviceUuid: Uuid,
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(serviceUuid, deviceId, "DFU", onReady) {
    override val profile: ServiceType = ServiceType.DFU

    val repository = DFURepository()

    override fun prepare(service: RemoteService) {
        // DFU is detected by service presence only; no specific characteristic is required.
    }

    override suspend fun CoroutineScope.initialize() {
        val dfuType = when (serviceUuid) {
            DFU_SERVICE_UUID -> DFUsAvailable.DFU_SERVICE
            SMP_SERVICE_UUID -> DFUsAvailable.SMP_SERVICE
            LEGACY_DFU_SERVICE_UUID -> DFUsAvailable.LEGACY_DFU_SERVICE
            EXPERIMENTAL_BUTTONLESS_DFU_SERVICE_UUID -> DFUsAvailable.EXPERIMENTAL_BUTTONLESS_DFU_SERVICE
            MDS_SERVICE_UUID -> DFUsAvailable.MDS_SERVICE
            else -> return
        }
        Timber.tag("DFU").log(Log.Level.APPLICATION, "$dfuType found")
        try {
            repository.updateAppName(dfuType)
        } catch (_: Exception) {
            repository.clear()
        }
        onReady(this@DFUManager)
    }
}
