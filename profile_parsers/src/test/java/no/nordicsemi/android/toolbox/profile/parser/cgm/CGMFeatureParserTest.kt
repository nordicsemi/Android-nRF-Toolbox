package no.nordicsemi.android.toolbox.profile.parser.cgm

import no.nordicsemi.android.toolbox.profile.parser.cgms.CGMFeatureParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import no.nordicsemi.kotlin.data.ByteOrder

class CGMFeatureParserTest {

    @Test
    fun `valid input with E2E CRC supported and non-matching CRC`() {
        val data = byteArrayOf(0x01, 0x00, 0x10, 0x12, 0xFF.toByte(), 0xEE.toByte()) // Example data
        val result = CGMFeatureParser.parse(data, ByteOrder.LITTLE_ENDIAN)
        assertNull(result) // CRC mismatch should return null
    }


    @Test
    fun `valid input without E2E CRC and expected CRC as 0xFFFF`() {
        val data = byteArrayOf(0x00, 0x00, 0x10, 0x12, 0xFF.toByte(), 0xFF.toByte())
        val result = CGMFeatureParser.parse(data, ByteOrder.LITTLE_ENDIAN)
        assertNotNull(result)
        result?.features?.e2eCrcSupported?.let { assertFalse(it) }
    }

    @Test
    fun `invalid input - byte array size not equal to 6`() {
        val data = byteArrayOf(0x01, 0x00, 0x10) // Too short
        val result = CGMFeatureParser.parse(data)
        assertNull(result)
    }

    @Test
    fun `type and sample location parsing`() {
        val data = byteArrayOf(0x01, 0x00, 0x10, 0x21, 0xFF.toByte(), 0xFF.toByte())
        val result = CGMFeatureParser.parse(data)

        assertNotNull(result)
        assertEquals(1, result?.type)
        assertEquals(2, result?.sampleLocation)
    }
}

