package no.nordicsemi.android.toolbox.profile.data

import no.nordicsemi.kotlin.ble.core.ConnectionState.Disconnected.Reason

fun Reason.displayMessage(): String =
    when (this) {
        Reason.Cancelled -> "Connection was cancelled."
        Reason.LinkLoss -> "The device got out of range or has turned off."
        Reason.Success -> "Device disconnected successfully."
        Reason.TerminateLocalHost -> "Device disconnected by the local host."
        Reason.TerminatePeerUser -> "Device disconnected by the peer user."
        Reason.RequiredServiceNotFound -> "Required service not found." // This should not happen in nRF Toolbox, not service is required.
        is Reason.Timeout -> "Connection attempt timed out with ${this.duration}."
        is Reason.Unknown -> "Oops...! Connection went on a coffee break."
        Reason.UnsupportedAddress -> "Device disconnected due to unsupported address."
        Reason.InsufficientAuthentication -> "Device disconnected due to insufficient authentication."
        Reason.UnsupportedConfiguration -> "Connection attempt was aborted due PHY negotiations failure."
    }