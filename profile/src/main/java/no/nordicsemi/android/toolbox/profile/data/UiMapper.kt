package no.nordicsemi.android.toolbox.profile.data

import no.nordicsemi.kotlin.ble.core.ConnectionState.Disconnected.Reason

fun Reason.displayMessage(): String =
    when (this) {
        Reason.Success -> "Device disconnected successfully."
        Reason.Cancelled -> "Connection was cancelled."
        Reason.LinkLoss -> "The device got out of range or has turned off."
        Reason.TerminateLocalHost -> "Connection terminated by the local host."
        Reason.TerminatePeerUser -> "Connection terminated by the peer."
        Reason.RequiredServiceNotFound -> "Required service not found." // This should not happen in nRF Toolbox, not service is required.
        is Reason.Timeout -> "Connection attempt timed out after ${this.duration}."
        is Reason.Unknown -> "Error code: $status"
        Reason.UnsupportedAddress -> "Unsupported address type."
        Reason.InsufficientAuthentication -> "Insufficient authentication.\nBond information was removed on peer."
        Reason.UnsupportedConfiguration -> "PHY negotiations might have failed."
    }