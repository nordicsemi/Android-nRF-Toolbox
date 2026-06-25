package no.nordicsemi.android.toolbox.profile.parser.hrs

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder

object BodySensorLocationParser {

    fun parse(bytes: ByteArray): Int? {
        if (bytes.isEmpty()) return null

        return bytes.getInt(0, IntFormat.UINT8, ByteOrder.LITTLE_ENDIAN)
    }
}