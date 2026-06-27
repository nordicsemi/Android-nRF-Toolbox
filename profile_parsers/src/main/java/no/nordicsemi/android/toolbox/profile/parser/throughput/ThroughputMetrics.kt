package no.nordicsemi.android.toolbox.profile.parser.throughput

data class ThroughputMetrics(
    val gattWritesReceived: Long = 0,      // Number of GATT writes received
    val totalBytesReceived: Long = 0,     // Total bytes received
    val throughputBitsPerSecond: Long = 0 // Throughput in bits per second
)