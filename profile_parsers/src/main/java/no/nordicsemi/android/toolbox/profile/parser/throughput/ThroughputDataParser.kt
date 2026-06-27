package no.nordicsemi.android.toolbox.profile.parser.throughput

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder

object ThroughputDataParser {

    fun parse(data: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): ThroughputMetrics? {
        if (data.size != 12) return null

        var offset = 0

        val numberOfGattWrite = data.getInt(offset, IntFormat.UINT32, byteOrder).toLong()
            .also { offset += 4 }
        val totalBytesReceived = data.getInt(offset, IntFormat.UINT32, byteOrder).toLong()
            .also { offset += 4 }
        val throughput = data.getInt(offset, IntFormat.UINT32, byteOrder).toLong()

        return ThroughputMetrics(
            gattWritesReceived = numberOfGattWrite,
            totalBytesReceived = totalBytesReceived,
            throughputBitsPerSecond = throughput,
        )
    }
}
