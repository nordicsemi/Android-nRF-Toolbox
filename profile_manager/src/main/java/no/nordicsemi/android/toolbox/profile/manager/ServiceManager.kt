package no.nordicsemi.android.toolbox.profile.manager

import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.kotlin.ble.client.Profile
import kotlin.uuid.Uuid

/**
 * Base class for all service managers.
 *
 * Each manager implements a BLE GATT profile as a [Profile.Simple], meaning it is registered
 * on the [no.nordicsemi.kotlin.ble.client.android.Peripheral] with `required = false`.
 * The [prepare] method validates required characteristics using [first] and [require].
 * If validation fails, the profile silently does not activate for that device.
 * The [initialize] method sets up subscriptions and reads initial values.
 *
 * @param serviceUuid The UUID of the GATT service this manager handles.
 * @param deviceId The address of the connected peripheral.
 * @param name A human-readable name for logging.
 * @param onReady Callback invoked after [initialize] completes, used to update the UI.
 */
abstract class ServiceManager(
    serviceUuid: Uuid,
    protected val deviceId: String,
    name: String? = null,
    protected val onReady: (ServiceManager) -> Unit,
) : Profile.Simple(serviceUuid, name) {

    /** Stable identity for this manager instance, used as the Compose/ViewModel key. */
    val instanceId: String = "${deviceId}_${serviceUuid}_${java.util.UUID.randomUUID()}"

    /** Returns the profile type enum used by the UI to select the correct screen. */
    abstract val profile: ServiceType
}
