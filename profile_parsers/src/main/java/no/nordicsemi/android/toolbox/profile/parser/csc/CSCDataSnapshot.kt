package no.nordicsemi.android.toolbox.profile.parser.csc

internal data class CSCDataSnapshot(
    var wheelRevolutions: Long = -1,
    var wheelEventTime: Int = -1,
    var crankRevolutions: Long = -1,
    var crankEventTime: Int = -1
)