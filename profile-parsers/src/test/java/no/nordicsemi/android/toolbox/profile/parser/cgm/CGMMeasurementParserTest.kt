package no.nordicsemi.android.toolbox.profile.parser.cgm

import no.nordicsemi.android.toolbox.profile.parser.cgms.CGMMeasurementParser
import no.nordicsemi.android.toolbox.profile.parser.common.CRC16
import org.junit.Assert.*
import org.junit.Test
import no.nordicsemi.kotlin.data.ByteOrder

class CGMMeasurementParserTest {

    @Test
    fun `test valid data with all optional fields`() {
        // Constructing a valid byte array
        val data = byteArrayOf(
            0x0F,  // Size: 14 bytes (6 base + 2 trend + 2 quality + 1 warning + 1 temp + 1 status + 2 CRC)
            0xE3.toByte(),  // Flags: All optional fields present (binary 11100011)
            0x78, 0x00,  // Glucose concentration: 120 mg/dL
            0x1E, 0x00,  // Time offset: 30 minutes
            0x01,  // Warning status
            0x02,  // Calibration temp status
            0x03,  // Sensor status
            0x50, 0x00,  // Trend: 80 mg/dL/min
            0x60, 0x00,  // Quality: 96 mg/dL
            0x34, 0x12  // CRC: Placeholder (valid CRC for this data)
        )

        // Calculate correct CRC and update the placeholder
        val expectedCrc = CRC16.MCRF4XX(data, 0, 12) // Excluding the CRC bytes
        data[data.size - 2] = (expectedCrc and 0xFF).toByte()
        data[data.size - 1] = (expectedCrc shr 8).toByte()

        val records = CGMMeasurementParser.parse(data, ByteOrder.LITTLE_ENDIAN)
        assertNotNull(records)
        assertEquals(1, records!!.size)

        val record = records[0]
        assertEquals(120f, record.glucoseConcentration)
        assertEquals(30, record.timeOffset)
        assertNotNull(record.status)
        assertTrue(record.status!!.deviceSpecificAlert)
        assertTrue(record.status.calibrationRequired)
        assertTrue(record.status.sensorMalfunction)
        assertEquals(80f, record.trend!!)
        assertEquals(96f, record.quality!!)
        assertTrue(record.crcPresent)
    }

    @Test
    fun `test valid data without optional fields`() {
        val data = byteArrayOf(
            0x06,  // Size: 6 bytes (base packet size without any optional fields)
            0x00,  // Flags: No optional fields present
            0x78, 0x00,  // Glucose concentration: 120 mg/dL
            0x1E, 0x00  // Time offset: 30 minutes
        )

        val records = CGMMeasurementParser.parse(data, ByteOrder.LITTLE_ENDIAN)
        assertNotNull(records)
        assertEquals(1, records!!.size)

        val record = records[0]
        assertEquals(120f, record.glucoseConcentration)
        assertEquals(30, record.timeOffset)
        assertNull(record.status)
        assertNull(record.trend)
        assertNull(record.quality)
        assertFalse(record.crcPresent)
    }

    @Test
    fun `test invalid data with incorrect size`() {
        val data = byteArrayOf(
            0x05,  // Size: 5 bytes (less than minimum size of 6)
            0x00,  // Flags: No optional fields present
            0x78, 0x00,  // Glucose concentration: 120 mg/dL
            0x1E  // Incomplete time offset
        )

        val records = CGMMeasurementParser.parse(data, ByteOrder.LITTLE_ENDIAN)
        assertNull(records)
    }

    @Test
    fun `test invalid data with mismatched CRC`() {
        val data = byteArrayOf(
            0x08,  // Size: 8 bytes (6 base + 2 CRC)
            0x00,  // Flags: No optional fields present
            0x78, 0x00,  // Glucose concentration: 120 mg/dL
            0x1E, 0x00,  // Time offset: 30 minutes
            0x12, 0x34  // CRC: Invalid placeholder
        )

        val records = CGMMeasurementParser.parse(data, ByteOrder.LITTLE_ENDIAN)
        assertNull(records)
    }

    @Test
    fun `test empty data`() {
        val data = byteArrayOf()
        val records = CGMMeasurementParser.parse(data, ByteOrder.LITTLE_ENDIAN)
        assertNull(records)
    }
}
