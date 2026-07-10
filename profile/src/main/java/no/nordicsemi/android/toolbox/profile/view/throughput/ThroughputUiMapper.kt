package no.nordicsemi.android.toolbox.profile.view.throughput

import no.nordicsemi.android.toolbox.profile.parser.throughput.ThroughputMetrics
import no.nordicsemi.android.toolbox.profile.data.NumberOfBytes
import no.nordicsemi.android.toolbox.profile.data.NumberOfSeconds
import java.util.Locale

internal fun ThroughputMetrics.throughputDataReceived(): String {
    val kilobytes = this.totalBytesReceived / 1024f
    val megabytes = kilobytes / 1024f

    return when {
        megabytes >= 1 -> {
            "${String.format(Locale.US, "%.2f", megabytes)} mB"
        }

        kilobytes > 0 -> {
            "${String.format(Locale.US, "%.2f", kilobytes)} kB"
        }

        else -> {
            "${this.totalBytesReceived} bytes"
        }
    }
}

internal fun ThroughputMetrics.displayThroughput(): String {
    val kbps = this.throughputBitsPerSecond / 8192f
    return String.format(Locale.US, "%.2f", kbps)
}

/**
 * This function returns a list of Strings from the Throughput input type.
 */
fun getThroughputInputTypes(): List<String> {
    return listOf(
        NumberOfBytes.getString(),
        NumberOfSeconds.getString()
    )
}
