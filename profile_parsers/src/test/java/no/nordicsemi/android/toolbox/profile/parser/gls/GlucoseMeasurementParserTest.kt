package no.nordicsemi.android.toolbox.profile.parser.gls

import no.nordicsemi.android.toolbox.profile.parser.gls.data.ConcentrationUnit
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GlucoseStatus
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RecordType
import no.nordicsemi.android.toolbox.profile.parser.gls.data.SampleLocation
import no.nordicsemi.kotlin.data.ByteOrder
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GlucoseMeasurementParserNewTest {

    @Test
    fun `parse should return null for data size less than 10`() {
        val data = ByteArray(9) { 0x00.toByte() }
        val result = GlucoseMeasurementParser.parse(data)
        assertNull(result, "Expected null for data size less than 10")
    }

    @Test
    fun `parse should parse record with only required fields`() {
        val data = byteArrayOf(
            0x00.toByte(),                  // Flags: No optional fields
            0x01.toByte(), 0x00.toByte(),   // Sequence Number
            0xE4.toByte(), 0x07.toByte(),   // Year: 2020 (little-endian)
            0x05.toByte(),                  // Month: May
            0x15.toByte(),                  // Day: 21
            0x0A.toByte(),                  // Hour: 10
            0x1E.toByte(),                  // Minute: 30
            0x2D.toByte()                   // Second: 45
        )
        val expectedBaseTime = Calendar.getInstance().apply {
            set(2020, Calendar.MAY, 21, 10, 30, 45)
            set(Calendar.MILLISECOND, 0)
        }

        val result = GlucoseMeasurementParser.parse(data)

        assertEquals(1, result?.sequenceNumber)
        assertEquals(expectedBaseTime, result?.time)
        assertNull(result?.glucoseConcentration)
        assertNull(result?.unit)
        assertNull(result?.status)
        assertNull(result?.type)
        assertNull(result?.sampleLocation)
        assertEquals(false, result?.contextInformationFollows)
    }

    @Test
    fun `parse should parse record with all optional fields`() {
        val data = byteArrayOf(
            0x1F.toByte(),  // Flags: All optional fields present
            0x02.toByte(),
            0x00.toByte(),  // Sequence Number
            0xE4.toByte(),
            0x07.toByte(),  // Year: 2020 (little-endian)
            0x05.toByte(),  // Month: May
            0x15.toByte(),  // Day: 21
            0x0A.toByte(),  // Hour: 10
            0x1E.toByte(),  // Minute: 30
            0x2D.toByte(),  // Second: 45
            0x00.toByte(),
            0x00.toByte(),  // Time Offset: 0 minutes
            0x51.toByte(),
            0x00.toByte(),
            0x14.toByte(),  // Glucose concentration (IEEE 11073 format) and type/sample location
            0x06.toByte(),
            0x00.toByte(),  // Sensor Status Annunciation
        )
        val expectedBaseTime = Calendar.getInstance().apply {
            set(2020, Calendar.MAY, 21, 10, 30, 45)
            set(Calendar.MILLISECOND, 0)
        }

        val result = GlucoseMeasurementParser.parse(data)

        assertEquals(2, result?.sequenceNumber)
        assertEquals(expectedBaseTime, result?.time)
        result?.glucoseConcentration?.let {
            assertEquals(
                81.0f,
                it, 0.01f
            )
        }
        assertEquals(ConcentrationUnit.UNIT_MOLPL, result?.unit)
        assertEquals(RecordType.VENOUS_PLASMA, result?.type)
        assertEquals(SampleLocation.FINGER, result?.sampleLocation)
        assertEquals(true, result?.contextInformationFollows)
        assertEquals(GlucoseStatus(6).toString(), result?.status.toString())

    }

    @Test
    fun `parse should return null for incomplete optional fields`() {
        val data = byteArrayOf(
            0x0F,                           // Flags: All optional fields except context present
            0x03, 0x00,                     // Sequence Number
            0xE4.toByte(), 0x07.toByte(),   // Year: 2020 (little-endian)
            0x05.toByte(),                  // Month: May
            0x15.toByte(),                  // Day: 21
            0x0A.toByte(),                  // Hour: 10
            0x1E.toByte(),                  // Minute: 30
            0x2D.toByte(),                  // Second: 45
            0x05, 0x00,                     // Time Offset
            0xCD.toByte(), 0xCC.toByte()    // Incomplete glucose concentration field
        )
        val result = GlucoseMeasurementParser.parse(data)
        assertNull(result, "Expected null for incomplete optional fields")
    }

    @Test
    fun `parse should handle little endian and big endian byte orders`() {
        val littleEndianData = byteArrayOf(
            0x00,                           // Flags
            0x01, 0x00,                     // Sequence Number (Little Endian: 1)
            0xE4.toByte(), 0x07.toByte(),   // Year: 2020 (little-endian)
            0x05.toByte(),                  // Month: May
            0x15.toByte(),                  // Day: 21
            0x0A.toByte(),                  // Hour: 10
            0x1E.toByte(),                  // Minute: 30
            0x2D.toByte(),                  // Second: 45
        )
        val bigEndianData = byteArrayOf(
            0x00,                           // Flags
            0x00, 0x01,                     // Sequence Number (Big Endian: 1)
            0xE4.toByte(), 0x07.toByte(),   // Year: 2020 (little-endian)
            0x05.toByte(),                  // Month: May
            0x15.toByte(),                  // Day: 21
            0x0A.toByte(),                  // Hour: 10
            0x1E.toByte(),                  // Minute: 30
            0x2D.toByte(),                  // Second: 45
        )

        val littleEndianResult = GlucoseMeasurementParser.parse(littleEndianData)
        val bigEndianResult = GlucoseMeasurementParser.parse(bigEndianData)

        assertEquals(1, littleEndianResult?.sequenceNumber)
        assertEquals(1, bigEndianResult?.sequenceNumber)
    }
}
