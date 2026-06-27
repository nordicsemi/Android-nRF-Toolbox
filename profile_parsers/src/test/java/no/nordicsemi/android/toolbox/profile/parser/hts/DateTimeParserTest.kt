package no.nordicsemi.android.toolbox.profile.parser.hts

import no.nordicsemi.android.toolbox.profile.parser.date.DateTimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class DateTimeParserTest {

    @Test
    fun `test parse with valid input`() {
        val byteArray = byteArrayOf(
            0xE4.toByte(), 0x07.toByte(), // Year: 2020 (little-endian)
            0x05.toByte(),               // Month: May
            0x15.toByte(),               // Day: 21
            0x0A.toByte(),               // Hour: 10
            0x1E.toByte(),               // Minute: 30
            0x2D.toByte()                // Second: 45
        )
        val offset = 0

        val calendar = DateTimeParser.parse(byteArray, offset)

        assertNotNull(calendar)
        calendar?.let {
            assertEquals(2020, it.get(Calendar.YEAR))
            assertEquals(Calendar.MAY, it.get(Calendar.MONTH))
            assertEquals(21, it.get(Calendar.DATE))
            assertEquals(10, it.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, it.get(Calendar.MINUTE))
            assertEquals(45, it.get(Calendar.SECOND))
        }
    }

    @Test
    fun `test parse with insufficient byte array`() {
        val byteArray = byteArrayOf(
            0xE4.toByte(), 0x07.toByte(), // Year: 2020 (little-endian)
            0x05.toByte()                // Incomplete data
        )
        val offset = 0

        val calendar = DateTimeParser.parse(byteArray, offset)

        assertNull(calendar)
    }

    @Test
    fun `test parse with zeroed fields`() {
        val byteArray = byteArrayOf(
            0x00.toByte(), 0x00.toByte(), // Year: 0 (invalid)
            0x00.toByte(),               // Month: 0 (invalid)
            0x00.toByte(),               // Day: 0 (invalid)
            0x0A.toByte(),               // Hour: 10
            0x1E.toByte(),               // Minute: 30
            0x2D.toByte()                // Second: 45
        )
        val offset = 0

        val calendar = DateTimeParser.parse(byteArray, offset)

        assertNotNull(calendar)

        calendar?.let {
            assertFalse(it.isSet(Calendar.YEAR))
            assertFalse(it.isSet(Calendar.MONTH))
            assertFalse(it.isSet(Calendar.DATE))
            assertEquals(10, it.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, it.get(Calendar.MINUTE))
            assertEquals(45, it.get(Calendar.SECOND))
        }
    }
}
