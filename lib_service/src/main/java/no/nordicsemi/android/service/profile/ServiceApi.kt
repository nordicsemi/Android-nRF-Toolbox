package no.nordicsemi.android.service.profile

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.log.ILogSession
import no.nordicsemi.android.toolbox.profile.manager.ServiceManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.ConnectionState

/**
 * Represents the public-facing API for the ProfileService.
 */
interface ServiceApi {

    /** A data class to hold all relevant information about a connected device. */
    data class DeviceData(
        val peripheral: Peripheral,
        val connectionState: ConnectionState = ConnectionState.Connecting,
        val services: List<ServiceManager> = emptyList(),
        val notSupported: Boolean? = null
    )

    /** A data class to represent a disconnection event. */
    data class DisconnectionEvent(
        val address: String,
        val reason: ConnectionState.Disconnected.Reason = ConnectionState.Disconnected.Reason.Success
    )

    /**
     * A flow that emits the current state of all managed devices.
     * The map key is the device address.
     */
    val devices: StateFlow<Map<String, DeviceData>>

    /**
     * A flow that emits the reason for the last disconnection event for any device.
     */
    val disconnectionEvent: SharedFlow<DisconnectionEvent>

    /**
     * Disconnects from a Bluetooth device and stops managing it.
     *
     * @param address The address of the device to disconnect from.
     */
    fun disconnect(address: String)

    /**
     * Retrieves a peripheral instance by its address.
     *
     * @param address The device address.
     * @return The [Peripheral] instance, or null if not found.
     */
    fun getPeripheral(address: String): Peripheral

    /**
     * Retrieves the log session for a specific device.
     */
    fun getLogSession(address: String): ILogSession?

    /**
     * Initiates and waits for the bonding process to complete with a device.
     *
     * @param address The device address.
     */
    suspend fun createBond(address: String)

    /**
     * Removes the bond information associated with this device.
     */
    suspend fun forget(address: String)
}