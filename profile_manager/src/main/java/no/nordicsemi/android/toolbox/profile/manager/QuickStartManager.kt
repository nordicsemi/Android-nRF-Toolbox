package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.android.toolbox.lib.utils.spec.QUICK_START_SERVICE_UUID
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

/**
 * The Quick Start service has no characteristics — its presence alone is used to
 * trigger the onboarding card for the "Quick Start" sample.
 */
class QuickStartManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(QUICK_START_SERVICE_UUID, deviceId, "Quick Start", onReady) {
    override val profile: ServiceType = ServiceType.QUICK_START

    override fun prepare(service: RemoteService) {
        // No characteristics to validate.
    }

    override suspend fun CoroutineScope.initialize() {
        onReady(this@QuickStartManager)
    }
}
