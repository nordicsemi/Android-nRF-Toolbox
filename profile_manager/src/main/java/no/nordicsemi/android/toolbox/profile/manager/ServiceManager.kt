package no.nordicsemi.android.toolbox.profile.manager

import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.Profile
import no.nordicsemi.kotlin.ble.client.RemoteService
import kotlin.uuid.Uuid

/**
 * Base class for all service managers.
 *
 * Each manager implements a BLE GATT profile and is registered on the
 * [no.nordicsemi.kotlin.ble.client.android.Peripheral] with `required = false`.
 * The [prepare] method validates required characteristics using [first] and [require].
 * If validation fails, the profile silently does not activate for that device.
 * The [initialize] method sets up subscriptions and reads initial values.
 *
 * Most profiles are based on a single service and extend [SimpleServiceManager]. A profile
 * based on multiple services, some of which may be optional, extends this class directly,
 * like [Profile.MultiService].
 *
 * @param requiredServiceUuids The UUIDs of the GATT services required by the profile.
 * @param optionalServiceUuids The UUIDs of the GATT services that are used if present.
 * @param deviceId The address of the connected peripheral.
 * @param name A human-readable name for logging.
 * @param onReady Callback invoked after [initialize] completes, used to update the UI.
 */
abstract class ServiceManager(
    requiredServiceUuids: List<Uuid>,
    optionalServiceUuids: List<Uuid> = emptyList(),
    protected val deviceId: String,
    name: String? = null,
    protected val onReady: (ServiceManager) -> Unit,
) : Profile.MultiService(requiredServiceUuids, optionalServiceUuids, name) {

    /** Stable identity for this manager instance, used as the Compose/ViewModel key. */
    val instanceId: String = "${deviceId}_${requiredServiceUuids.joinToString("_")}_${java.util.UUID.randomUUID()}"

    /** Returns the profile type enum used by the UI to select the correct screen. */
    abstract val profile: ServiceType
}

/**
 * Base class for the service managers based on a single GATT service.
 *
 * This works like [Profile.Simple].
 *
 * @param serviceUuid The UUID of the GATT service this manager handles.
 */
abstract class SimpleServiceManager(
    serviceUuid: Uuid,
    deviceId: String,
    name: String? = null,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(listOf(serviceUuid), emptyList(), deviceId, name, onReady) {

    final override fun prepare(peripheral: Peripheral<*, *>, services: List<RemoteService>) =
        prepare(peripheral, instance(services))

    /** Selects a single instance of the service. By default, the first one is used. */
    protected open fun instance(services: List<RemoteService>): RemoteService = services.first()

    /** Validates the [service] and stores the references to its characteristics. */
    protected abstract fun prepare(peripheral: Peripheral<*, *>, service: RemoteService)
}
