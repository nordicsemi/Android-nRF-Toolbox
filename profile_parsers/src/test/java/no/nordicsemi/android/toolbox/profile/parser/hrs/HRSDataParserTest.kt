package no.nordicsemi.android.toolbox.profile.parser.hrs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HRSDataParserTest {

    @Test
    fun `test parse with UINT8 heart rate and no additional data`() {
        val rawData = byteArrayOf(
            0x00.toByte(), // Flags: UINT8 heart rate, no sensor contact, no energy, no RR intervals
            0x64.toByte()  // Heart rate: 100
        )
        val result = HRSDataParser.parse(rawData)
        assertEquals(100, result?.heartRate)
        assertEquals(false, result?.sensorContact)
        assertEquals(null, result?.energyExpanded)
        assertEquals(emptyList<Int>(), result?.rrIntervals)
    }

    @Test
    fun `test parse with UINT16 heart rate and sensor contact`() {
        val rawData = byteArrayOf(
            0x07.toByte(),       // Flags: UINT16 heart rate, sensor contact supported
            0x64.toByte(),       // Byte array (little-endian): [0x64, 0x00] for 100
            0x00.toByte(),
        )
        val result = HRSDataParser.parse(rawData)
        assertEquals(100, result?.heartRate)
        assertEquals(true, result?.sensorContact)
        assertEquals(null, result?.energyExpanded)
        assertEquals(emptyList<Int>(), result?.rrIntervals)
    }

    @Test
    fun `test parse with energy expanded and RR intervals`() {
        val rawData = byteArrayOf(
            0x19.toByte(),       // Flags: UINT8 heart rate, energy expanded, RR intervals present
            0x4B.toByte(), 0x00.toByte(),    // Byte array (little-endian) Heart rate: 75
            0x38.toByte(), 0x04.toByte(), // Byte array (little-endian): 1080
            0x04.toByte(), 0x04.toByte(), //    Byte array (little-endian): 1028
            0x38.toByte(), 0x04.toByte()       // RR interval 2 MSB: 1080
        )
        val result = HRSDataParser.parse(rawData)
        assertEquals(75, result?.heartRate)
        assertEquals(false, result?.sensorContact)
        assertEquals(1080, result?.energyExpanded)
        assertEquals(listOf(1028, 1080), result?.rrIntervals)
    }

    @Test
    fun `test parse with insufficient data`() {
        val rawData = byteArrayOf(0x00.toByte()) // Only flags byte, insufficient for parsing
        val result = HRSDataParser.parse(rawData)
        assertNull(result)
    }

    @Test
    fun `test parse with unsupported flag combinations`() {
        val rawData = byteArrayOf(
            0xFF.toByte(),       // Flags: Unsupported combination
            0x64.toByte()        // Heart rate: 100
        )
        val result = HRSDataParser.parse(rawData)
        assertNull(result)
    }
}
