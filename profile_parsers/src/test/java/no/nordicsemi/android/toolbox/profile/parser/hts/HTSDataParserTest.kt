package no.nordicsemi.android.toolbox.profile.parser.hts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class HTSDataParserTest {

    @Test
    fun `test parse with all fields present`() {
        val rawData = byteArrayOf(
            0x06.toByte(),
            0x71.toByte(),       // Temperature byte 1 (LSB)
            0x0E.toByte(),       // Temperature byte 2
            0x00.toByte(),       // Temperature byte 3
            0xFE.toByte(),       // Temperature byte 4 (MSB)
            0xE4.toByte(),       // Year LSB (2020)
            0x07.toByte(),       // Year MSB (2020)
            0x05.toByte(),       // Month: May
            0x15.toByte(),       // Day: 21
            0x0A.toByte(),       // Hour: 10
            0x1E.toByte(),       // Minute: 30
            0x2D.toByte(),       // Second: 45
            0x01.toByte(),       // Type 1: Armpit
        )

        val result = HTSDataParser.parse(rawData)

        assertNotNull(result)
        result?.let {
            assertEquals(HTSMeasurementType.ARMPIT, HTSMeasurementType.fromValue(it.type!!))
            assertEquals(36.97f, it.temperature, 0.01f)
            assertEquals(TemperatureUnitData.CELSIUS, it.unit)
            assertNotNull(it.timestamp)
            assertEquals(2020, it.timestamp?.get(Calendar.YEAR))
            assertEquals(Calendar.MAY, it.timestamp?.get(Calendar.MONTH))
            assertEquals(21, it.timestamp?.get(Calendar.DATE))
            assertEquals(10, it.timestamp?.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, it.timestamp?.get(Calendar.MINUTE))
            assertEquals(45, it.timestamp?.get(Calendar.SECOND))
        }
    }

    @Test
    fun `test parse without optional fields`() {
        val rawData =
            byteArrayOf(0x00.toByte(), 0xC4.toByte(), 0x09.toByte(), 0x00.toByte(), 0xFE.toByte())

        val result = HTSDataParser.parse(rawData)

        assertNotNull(result)
        result?.let {
            assertEquals(25.0f, it.temperature, 0.01f)
            assertEquals(TemperatureUnitData.CELSIUS, it.unit)
            assertNull(it.timestamp)
            assertNull(it.type)
        }
    }

    @Test
    fun `test parse with infinity float`() {
        val byteArray = byteArrayOf(
            0x00.toByte(),            // Flags: Unit (Celsius), No Timestamp, No Temperature Type
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F.toByte() // Temperature: +Infinity
        )

        val result = HTSDataParser.parse(byteArray)

        assertEquals(Float.POSITIVE_INFINITY, result!!.temperature, 0.01f)
    }

    @Test
    fun `test parse with NaN float`() {
        val byteArray = byteArrayOf(
            0x00.toByte(),            // Flags: Unit (Celsius), No Timestamp, No Temperature Type
            0xFF.toByte(), 0xFF.toByte(), 0x7F.toByte(), 0x00.toByte() // Temperature: NaN
        )

        val result = HTSDataParser.parse(byteArray)

        assertEquals(Float.NaN, result!!.temperature, 0.01f)
    }

    @Test
    fun `test parse with insufficient byte array`() {
        val byteArray =
            byteArrayOf(0x07.toByte()) // Flags indicate more data, but size is insufficient

        val result = HTSDataParser.parse(byteArray)

        assertNull(result)
    }

    @Test
    fun `test parse with invalid flag`() {
        val byteArray = byteArrayOf(
            0x08.toByte(),            // Invalid Flag
            0xC4.toByte(), 0x09.toByte(), 0x00.toByte(), 0xFE.toByte() // Temperature: 25.0
        )

        val result = HTSDataParser.parse(byteArray)

        assertNull(result) // Should return null due to invalid unit flag
    }
}
