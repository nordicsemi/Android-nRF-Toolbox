package no.nordicsemi.android.toolbox.lib.utils

suspend fun tryOrLog(block: suspend () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        t.logAndReport()
    }
}
