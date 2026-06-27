package no.nordicsemi.android.toolbox.profile.parser.bps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class BloodPressureMeasurementParserTest {

    @Test
    fun `test parse valid data with all fields`() {
        val data = byteArrayOf(
            0x1f.toByte(),                          // Flags: All fields present
            0x79.toByte(), 0x00.toByte(),           // Systolic: 121
            0x51.toByte(), 0x00.toByte(),           // Diastolic: 81
            0x6a.toByte(), 0x00.toByte(),           // Mean Arterial Pressure: 106
            0xE4.toByte(),                          // Year LSB (2020)
            0x07.toByte(),                          // Year MSB (2020)
            0x05.toByte(),                          // Month: May
            0x15.toByte(),                          // Day: 21
            0x0A.toByte(),                          // Hour: 10
            0x1E.toByte(),                          // Minute: 30
            0x2D.toByte(),                          // Second: 45
            0x48.toByte(), 0x00.toByte(),           // Pulse Rate: 72.0 bpm
            0x01.toByte(),                          // User ID: 1
            0x06.toByte(), 0x00.toByte()            // Measurement Status: Irregular pulse detected
        )

        val result = BloodPressureMeasurementParser.parse(data)

        assertNotNull(result)
        assertEquals(121.0f, result?.systolic)
        assertEquals(81.0f, result?.diastolic)
        assertEquals(106.0f, result?.meanArterialPressure)
        assertEquals(BloodPressureType.UNIT_KPA, result?.unit)
        assertEquals(72.0f, result?.pulseRate)
        assertEquals(1, result?.userID)
        assertNotNull(result?.status)
        assertEquals(true, result?.status?.irregularPulseDetected)
        assertEquals(Calendar.MAY, result?.calendar?.get(Calendar.MONTH))
        assertEquals(2020, result?.calendar?.get(Calendar.YEAR))
    }

    @Test
    fun `test parse valid data without optional fields`() {
        val data = byteArrayOf(
            0x00.toByte(),                          // Flags: No optional fields
            0x48.toByte(), 0x00.toByte(),           // Systolic: 72.0 mmHg
            0x51.toByte(), 0x00.toByte(),           // Diastolic: 81.0 mmHg
            0x40.toByte(), 0x00.toByte()            // Mean Arterial Pressure: 64.0 mmHg
        )

        val result = BloodPressureMeasurementParser.parse(data)

        assertNotNull(result)
        assertEquals(72.0f, result?.systolic)
        assertEquals(81.0f, result?.diastolic)
        assertEquals(64.0f, result?.meanArterialPressure)
        assertEquals(BloodPressureType.UNIT_MMHG, result?.unit)
        assertNull(result?.pulseRate)
        assertNull(result?.userID)
        assertNull(result?.status)
        assertNull(result?.calendar)
    }

    @Test
    fun `test parse invalid data length`() {
        val data = byteArrayOf(
            0x1F.toByte(),  // Flags indicating all fields present
            0x00, 0x48     // Insufficient data
        )

        val result = BloodPressureMeasurementParser.parse(data)

        assertNull(result)
    }

    @Test
    fun `test parse with missing timestamp and pulse rate`() {
        val data = byteArrayOf(
            0x08.toByte(),                              // Flags: Only systolic, diastolic, mean arterial pressure present
            0x48.toByte(), 0x00.toByte(),               // Systolic: 72.0 mmHg
            0x51.toByte(), 0x00.toByte(),               // Diastolic: 81.0 mmHg
            0x40.toByte(), 0x00.toByte(),               // Mean Arterial Pressure: 64.0 mmHg
            0x01                                        // User ID: 1
        )

        val result = BloodPressureMeasurementParser.parse(data)

        assertNotNull(result)
        assertEquals(72.0f, result?.systolic)
        assertEquals(81.0f, result?.diastolic)
        assertEquals(64.0f, result?.meanArterialPressure)
        assertNull(result?.calendar)
        assertNull(result?.pulseRate)
        assertEquals(1, result?.userID)
    }

    @Test
    fun `test parse null data`() {
        val result = BloodPressureMeasurementParser.parse(ByteArray(0))
        assertNull(result)
    }
}
