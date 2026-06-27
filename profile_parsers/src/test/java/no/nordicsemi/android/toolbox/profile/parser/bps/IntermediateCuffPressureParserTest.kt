package no.nordicsemi.android.toolbox.profile.parser.bps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import no.nordicsemi.kotlin.data.ByteOrder
import java.util.Calendar

class IntermediateCuffPressureParserTest {

    @Test
    fun `parse valid data with all flags set`() {
        val data = byteArrayOf(
            0x1F.toByte(),                          // Flags: all features enabled
            0x51.toByte(), 0x00.toByte(),           // Cuff pressure (81.0 mmHg)
            0x00.toByte(), 0x00.toByte(),           // following bytes - cuff pressure. Diastolic and MAP are unused
            0x00.toByte(), 0x00.toByte(),
            0xE4.toByte(),                          // Year LSB (2020)
            0x07.toByte(),                          // Year MSB (2020)
            0x05.toByte(),                          // Month: May
            0x15.toByte(),                          // Day: 21
            0x0A.toByte(),                          // Hour: 10
            0x1E.toByte(),                          // Minute: 30
            0x2D.toByte(),                          // Second: 45
            0x64.toByte(), 0x00.toByte(),           // Pulse rate (100 bpm)
            0x01.toByte(),                          // User ID (1)
            0x06.toByte(), 0x00.toByte()            // Measurement status
        )

        val result = IntermediateCuffPressureParser.parse(data)

        assertNotNull(result)
        assertEquals(81.0f, result?.cuffPressure)
        assertEquals(BloodPressureType.UNIT_KPA, result?.unit)
        assertNotNull(result?.calendar)
        assertEquals(100.0f, result?.pulseRate)
        assertEquals(1, result?.userID)
        assertNotNull(result?.status)
        assertEquals(Calendar.MAY, result?.calendar?.get(Calendar.MONTH))
        assertEquals(2020, result?.calendar?.get(Calendar.YEAR))
    }

    @Test
    fun `parse valid data with no optional fields`() {
        val data = byteArrayOf(
            0x01.toByte(),                      // Flags: no optional fields
            0x51.toByte(), 0x00.toByte(),       // Cuff pressure (81.0 mmHg)
            0x00.toByte(), 0x00.toByte(),       // following bytes - cuff pressure. Diastolic and MAP are unused
            0x00.toByte(), 0x00.toByte(),
        )

        val result = IntermediateCuffPressureParser.parse(data)

        assertNotNull(result)
        assertEquals(81.0f, result?.cuffPressure)
        assertEquals(BloodPressureType.UNIT_KPA, result?.unit)
        assertNull(result?.calendar)
        assertNull(result?.pulseRate)
        assertNull(result?.userID)
        assertNull(result?.status)
    }

    @Test
    fun `parse data with insufficient length`() {
        val data = byteArrayOf(
            0x00.toByte(), // Flags: no optional fields
            0x34.toByte() // Incomplete cuff pressure
        )

        val result = IntermediateCuffPressureParser.parse(data, ByteOrder.LITTLE_ENDIAN)

        assertNull(result)
    }

    @Test
    fun `parse valid data with timestamp only`() {
        val data = byteArrayOf(
            0x02.toByte(),                           // Flags: timestamp present
            0x51.toByte(), 0x00.toByte(),           // Cuff pressure (81.0 mmHg)
            0x00.toByte(), 0x00.toByte(),           // Following bytes - cuff pressure. Diastolic and MAP are unused
            0x00.toByte(), 0x00.toByte(),
            0xE4.toByte(),                          // Year LSB (2020)
            0x07.toByte(),                          // Year MSB (2020)
            0x05.toByte(),                          // Month: May
            0x15.toByte(),                          // Day: 21
            0x0A.toByte(),                          // Hour: 10
            0x1E.toByte(),                          // Minute: 30
            0x2D.toByte(),                          // Second: 45
        )

        val result = IntermediateCuffPressureParser.parse(data, ByteOrder.LITTLE_ENDIAN)

        assertNotNull(result)
        assertEquals(81.0f, result?.cuffPressure)
        assertNotNull(result?.calendar)
        assertNull(result?.pulseRate)
        assertNull(result?.userID)
        assertNull(result?.status)
    }

    @Test
    fun `parse valid data with kPa unit`() {
        val data = byteArrayOf(
            0x01.toByte(),                  // Flags: kPa unit, no optional fields
            0x51.toByte(), 0x00.toByte(),  // Cuff pressure (81.0 mmHg)
            0x00.toByte(), 0x00.toByte(),  // Following bytes - cuff pressure. Diastolic and MAP are unused
            0x00.toByte(), 0x00.toByte(),
        )

        val result = IntermediateCuffPressureParser.parse(data, ByteOrder.LITTLE_ENDIAN)

        assertNotNull(result)
        assertEquals(81.0f, result?.cuffPressure)
        assertEquals(BloodPressureType.UNIT_KPA, result?.unit)
        assertNull(result?.calendar)
        assertNull(result?.pulseRate)
        assertNull(result?.userID)
        assertNull(result?.status)
    }
}
