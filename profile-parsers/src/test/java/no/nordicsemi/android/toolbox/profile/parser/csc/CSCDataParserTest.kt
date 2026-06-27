package no.nordicsemi.android.toolbox.profile.parser.csc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CSCDataParserTest {

    @Before
    fun setup() {
        // Reset the parser state before each test
        CSCDataParser.previousData = CSCDataSnapshot()
        CSCDataParser.wheelRevolutions = -1
        CSCDataParser.wheelEventTime = -1
        CSCDataParser.crankRevolutions = -1
        CSCDataParser.crankEventTime = -1
    }

    @Test
    fun `test parse with empty data returns null`() {
        val result = CSCDataParser.parse(byteArrayOf())
        assertNull(result)
    }

    @Test
    fun `test parse with invalid data length returns null`() {
        val invalidData = byteArrayOf(0x01) // Insufficient data
        val result = CSCDataParser.parse(invalidData)
        assertNull(result)
    }

    @Test
    fun `test parse with wheel revolutions only`() {
       val data = byteArrayOf(
            0x01.toByte(), 0x0A.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xA0.toByte(), 0x00.toByte()
        )
        val result = CSCDataParser.parse(data)

        assertEquals(0.0f, result?.distance) // No previous data to compare
        assertEquals(0.0f, result?.speed)
        assertEquals(0.0f, result?.cadence)
    }

    @Test
    fun `test parse with wheel and crank revolutions`() {
        val data = createData(
            flags = 0x03, // Both wheel and crank revolutions present
            wheelRevolutions = 1000,
            wheelEventTime = 200,
            crankRevolutions = 500,
            crankEventTime = 150
        )

        val result = CSCDataParser.parse(data)

        assertEquals(0.0f, result?.distance) // No previous data to compare
        assertEquals(0.0f, result?.speed)
        assertEquals(0.0f, result?.cadence)
    }

    @Test
    fun `test parse with sequential data updates distance and speed`() {
        val initialData = createData(
            flags = 0x01, // Wheel revolution present
            wheelRevolutions = 1000,
            wheelEventTime = 200
        )
        CSCDataParser.parse(initialData)

        val updatedData = createData(
            flags = 0x01,
            wheelRevolutions = 1100, // Increased revolutions
            wheelEventTime = 1000 // Increased event time
        )
        val result = CSCDataParser.parse(updatedData)

        assertEquals(234.0f, result?.distance) // Example calculation
        result?.speed?.let { assertEquals(299.52f, it, 0.1f) } // Example calculation
    }

    @Test
    fun `test parse calculates cadence and gear ratio`() {
        val initialData = createData(
            flags = 0x03, // Both wheel and crank revolutions present
            wheelRevolutions = 10,
            wheelEventTime = 200,
            crankRevolutions = 5,
            crankEventTime = 150
        )
        CSCDataParser.parse(initialData)

        val updatedData = createData(
            flags = 0x03,
            wheelRevolutions = 15,
            wheelEventTime = 300,
            crankRevolutions = 6,
            crankEventTime = 200
        )
        val result = CSCDataParser.parse(updatedData)

        result?.cadence?.let { assertEquals(1228.8f, it, 0.1f) } // Example calculation
        result?.gearRatio?.let { assertEquals(2.5f, it, 0.1f) } // Example calculation
    }

    private fun createData(
        flags: Int,
        wheelRevolutions: Int = 0,
        wheelEventTime: Int = 0,
        crankRevolutions: Int = 0,
        crankEventTime: Int = 0
    ): ByteArray {
        val buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(flags.toByte())
        if ((flags and 0x01) != 0) {
            buffer.putInt(wheelRevolutions)
            buffer.putShort(wheelEventTime.toShort())
        }
        if ((flags and 0x02) != 0) {
            buffer.putShort(crankRevolutions.toShort())
            buffer.putShort(crankEventTime.toShort())
        }
        return buffer.array()
    }
}
