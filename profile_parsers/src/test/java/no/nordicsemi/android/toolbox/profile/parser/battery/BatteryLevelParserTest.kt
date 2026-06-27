package no.nordicsemi.android.toolbox.profile.parser.battery

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BatteryLevelParserTest {

    @Test
    fun `test parse with valid UINT8 data`() {
        val data = byteArrayOf(75.toByte())
        val result = BatteryLevelParser.parse(data)
        assertEquals(75, result)
    }

    @Test
    fun `test parse with UINT8 data at maximum value`() {
        val data = byteArrayOf(255.toByte())
        val result = BatteryLevelParser.parse(data)
        assertEquals(255, result)
    }

    @Test
    fun `test parse with UINT8 data at minimum value`() {
        val data = byteArrayOf(0.toByte())
        val result = BatteryLevelParser.parse(data)
        assertEquals(0, result)
    }

    @Test
    fun `test parse with more than one byte returns null`() {
        val data = byteArrayOf(102.toByte(), 108.toByte())
        val result = BatteryLevelParser.parse(data)
        assertNull(result)
    }

    @Test
    fun `test parse with empty data returns null`() {
        val data = byteArrayOf()
        val result = BatteryLevelParser.parse(data)
        assertNull(result)
    }
}
