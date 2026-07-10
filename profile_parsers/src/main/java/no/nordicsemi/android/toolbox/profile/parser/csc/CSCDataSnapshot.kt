package no.nordicsemi.android.toolbox.profile.parser.csc

/**
 * @param wheelRevolutions the number of wheel revolutions since the last message.
 * @param wheelEventTime the time of the last message, in 1/1024 seconds.
 * @param crankRevolutions the number of crank revolutions since the last message.
 * @param crankEventTime the time of the last message, in 1/1024 seconds.
 */
internal data class CSCDataSnapshot(
    var wheelRevolutions: Long,
    var wheelEventTime: Int,
    var crankRevolutions: Long,
    var crankEventTime: Int
) {
    constructor(): this(-1, -1, -1, -1)
}