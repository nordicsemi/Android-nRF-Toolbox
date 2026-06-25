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

package no.nordicsemi.android.toolbox.profile.parser.date

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder
import java.util.Calendar

internal object DateTimeParser {

    fun parse(byte: ByteArray, offset: Int): Calendar? {
        if (byte.size < offset + 7) return null

        val calendar = Calendar.getInstance()
        val year = byte.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN)
        val month = byte.getInt(offset + 2, IntFormat.UINT8, ByteOrder.LITTLE_ENDIAN)
        val day = byte.getInt(offset + 3, IntFormat.UINT8, ByteOrder.LITTLE_ENDIAN)
        val hourOfDay = byte.getInt(offset + 4, IntFormat.UINT8, ByteOrder.LITTLE_ENDIAN)
        val minute = byte.getInt(offset + 5, IntFormat.UINT8, ByteOrder.LITTLE_ENDIAN)
        val second = byte.getInt(offset + 6, IntFormat.UINT8, ByteOrder.LITTLE_ENDIAN)

        if (year > 0) {
            calendar[Calendar.YEAR] = year
        } else {
            calendar.clear(Calendar.YEAR)
        }

        if (month > 0) {
            calendar[Calendar.MONTH] = month - 1
        } else {
            calendar.clear(Calendar.MONTH)
        }

        if (day > 0) {
            calendar[Calendar.DATE] = day
        } else {
            calendar.clear(Calendar.DATE)
        }

        calendar[Calendar.HOUR_OF_DAY] = hourOfDay
        calendar[Calendar.MINUTE] = minute
        calendar[Calendar.SECOND] = second
        calendar[Calendar.MILLISECOND] = 0

        return calendar
    }
}
