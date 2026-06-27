package no.nordicsemi.android.toolbox.profile.parser.prx

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt

object AlarmLevelParser {

    fun parse(data: ByteArray): AlarmLevel? {
        if (data.size == 1) {
            val level: Int = data.getInt(0, IntFormat.UINT8)
            return AlarmLevel.create(level)
        }
        return null
    }
}