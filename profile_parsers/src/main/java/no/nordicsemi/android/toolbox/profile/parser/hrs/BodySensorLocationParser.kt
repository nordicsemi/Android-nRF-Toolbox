package no.nordicsemi.android.toolbox.profile.parser.hrs

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder

object BodySensorLocationParser {

    fun parse(bytes: ByteArray): BodySensorLocation? {
        if (bytes.isEmpty()) return null

        val value = bytes.getInt(0, IntFormat.UINT8)
        return BodySensorLocation.create(value)
    }
}