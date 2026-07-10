package no.nordicsemi.android.toolbox.profile.parser.dis

object DeviceInformationParser {

    /** Decodes a UTF-8 string characteristic, trimming any trailing null terminator. */
    fun parseString(data: ByteArray): String? =
        data.decodeToString().trimEnd('\u0000').trim().ifEmpty { null }

    /** System ID: 5-octet manufacturer identifier + 3-octet organizationally unique identifier. */
    fun parseSystemId(data: ByteArray): String? {
        if (data.size != 8) return null
        return data.toHexString()
    }

    /** PnP ID: vendor ID source, vendor ID, product ID and product version. */
    fun parsePnpId(data: ByteArray): String? {
        if (data.size != 7) return null
        val vendorIdSource = when (data[0].toInt() and 0xFF) {
            1 -> "Bluetooth SIG"
            2 -> "USB Implementer's Forum"
            else -> "Unknown"
        }
        val vendorId = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        val productId = (data[3].toInt() and 0xFF) or ((data[4].toInt() and 0xFF) shl 8)
        val productVersion = (data[5].toInt() and 0xFF) or ((data[6].toInt() and 0xFF) shl 8)
        return "Vendor ID Source: $vendorIdSource,\nVendor ID: 0x%04X,\nProduct ID: 0x%04X,\nProduct Version: 0x%04X"
            .format(vendorId, productId, productVersion)
    }

    /** IEEE 11073-20601 Regulatory Certification Data List: opaque binary blob, shown as hex. */
    fun parseIeeeCertificationData(data: ByteArray): String? =
        data.takeIf { it.isNotEmpty() }?.let { "0x${it.toHexString()}" }
}
