package no.nordicsemi.android.toolbox.profile.manager

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.android.log.LogContract
import no.nordicsemi.android.observability.ObservabilityManager
import no.nordicsemi.android.observability.bluetooth.MonitoringAndDiagnosticsProfile
import no.nordicsemi.android.toolbox.lib.utils.spec.MDS_SERVICE_UUID
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.log.Log
import timber.log.Timber
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

class MDSManager(
    context: Context,
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(MDS_SERVICE_UUID, deviceId, "MDS", onReady) {
    override val profile: ServiceType = ServiceType.MDS

    val manager: ObservabilityManager = ObservabilityManager.create(context)

    // MonitoringAndDiagnosticsProfile is a class, so MDSManager can't extend it.
    // Instead, let's just all  `prepare` and `initialize` protected methods from this profile.
    // We must override the class to make them visible from outside.
    private val impl = object : MonitoringAndDiagnosticsProfile() {
        public override fun prepare(service: RemoteService) = super.prepare(service)
        suspend fun run(scope: CoroutineScope) = scope.initialize()
    }.apply {
        logger = Log.Sink { c, _, s, t, messageBuilder ->
            val tag = if (s != null) "${c.name} ($s)" else c.name
            Timber.tag(tag).log(LogContract.Log.Level.APPLICATION, t, messageBuilder())
        }
    }

    override fun prepare(service: RemoteService) {
        impl.prepare(service)
        manager.connect(impl)
        // Calling onReady will show the MDS view with Connecting... state.
        onReady(this@MDSManager)
    }

    override suspend fun CoroutineScope.initialize() {
        impl.run(this)
    }
}