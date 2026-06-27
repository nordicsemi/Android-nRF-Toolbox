package no.nordicsemi.android.toolbox.profile.parser.directionFinder.ddf

import kotlin.experimental.and

class DDFDataParser {

    fun parse(data: ByteArray): DDFData? {
        if (data.isEmpty()) return null

        var offset = 0
        val flags = data[offset].also { offset++ }

        val isRTTPresent = flags and 0x01 > 0
        val isMCPDPresent = flags and 0x02 > 0

        return DDFData(isMCPDPresent, isRTTPresent)
    }
}
