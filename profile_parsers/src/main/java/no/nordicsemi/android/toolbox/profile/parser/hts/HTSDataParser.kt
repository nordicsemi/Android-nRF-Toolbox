/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.toolbox.profile.parser.hts

import no.nordicsemi.android.toolbox.profile.parser.date.DateTimeParser
import no.nordicsemi.kotlin.data.FloatFormat
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getFloat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder
import java.util.Calendar

object HTSDataParser {

    fun parse(byte: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): HTSData? {
        if (byte.size < 5) return null

        var offset = 0
        val flag: Int = byte.getInt(offset, IntFormat.UINT8, byteOrder)

        if (flag > 7) return null

        val unit: TemperatureUnitData = TemperatureUnitData.create(flag and 0x01) ?: return null

        val timestampPresent = flag and 0x02 != 0

        val temperatureTypePresent = flag and 0x04 != 0
        offset += 1

        if (byte.size < 5 + (if (timestampPresent) 7 else 0) + (if (temperatureTypePresent) 1 else 0)) {
            return null
        }

        val temperature: Float = byte.getFloat(offset, FloatFormat.IEEE_11073_32_BIT, byteOrder)
        offset += 4

        var calendar: Calendar? = null
        if (timestampPresent) {
            calendar = DateTimeParser.parse(byte, offset)
            offset += 7
        }

        var type: Int? = null
        if (temperatureTypePresent) {
            type = byte.getInt(offset, IntFormat.UINT8, byteOrder)
            offset += 1
        }
        return HTSData(temperature, unit, calendar, type)
    }
}
