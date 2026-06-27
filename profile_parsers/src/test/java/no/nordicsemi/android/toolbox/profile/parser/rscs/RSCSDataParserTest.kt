package no.nordicsemi.android.toolbox.profile.parser.rscs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RSCSDataParserTest {

    @Test
    fun `test parse with all fields present`() {
        val data = byteArrayOf(
            0x07.toByte(),  // Flags: all fields present (0b00000111)
            0x80.toByte(),
            0x02.toByte(),  // Speed: 640 [0x0280] -> 2.5 m/s (640 / 256)
            0x50.toByte(),  // Cadence: 80
            0x20.toByte(),
            0x03.toByte(),  // Stride length: 800 [0x0320]
            0x00.toByte(),  // Total distance: 4096 [0x00100000]
            0x10.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )

        val result = RSCSDataParser.parse(data)

        // Validate parsed data
        assertNotNull(result)
        assertEquals(true, result?.running)             // Status: running
        assertEquals(2.5f, result?.instantaneousSpeed)  // Speed: 2.5 m/s
        assertEquals(80, result?.instantaneousCadence)  // Cadence: 80
        assertEquals(800, result?.strideLength)         // Stride length: 800 mm
        assertEquals(4096L, result?.totalDistance)      // Total distance: 4096 m
    }

    @Test
    fun `test parse with only mandatory fields`() {
        val data = byteArrayOf(
            0x00.toByte(),                  // Flags: no optional fields (0b00000000)
            0x80.toByte(), 0x02.toByte(),   // Speed: 640 [0x0280] -> 2.5 m/s (640 / 256)
            0x50.toByte()                   // Cadence: 80
        )

        val result = RSCSDataParser.parse(data)

        // Validate parsed data
        assertNotNull(result)
        assertEquals(false, result?.running)            // Status: not running
        assertEquals(2.5f, result?.instantaneousSpeed)  // Speed: 2.5 m/s
        assertEquals(80, result?.instantaneousCadence)  // Cadence: 80
        assertNull(result?.strideLength)                // Stride length: null
        assertNull(result?.totalDistance)               // Total distance: null
    }

    @Test
    fun `test parse with insufficient data`() {
        val data = byteArrayOf(
            0x00.toByte(),          // Flags: no optional fields
            0x80.toByte()          // Incomplete speed data
        )

        val result = RSCSDataParser.parse(data)

        assertNull(result) // Parsing should fail due to insufficient data
    }

    @Test
    fun `test parse with incorrect data size`() {
        val data = byteArrayOf(
            0x03.toByte(),                  // Flags: stride length present
            0x80.toByte(), 0x02.toByte(),  // Speed: 640 [0x0280]
            0x50.toByte(),                 // Cadence: 80
            0x20.toByte()                  // Incomplete stride length data
        )

        val result = RSCSDataParser.parse(data)

        // Validate parsed data
        assertNull(result) // Parsing should fail due to incorrect data size
    }
}
