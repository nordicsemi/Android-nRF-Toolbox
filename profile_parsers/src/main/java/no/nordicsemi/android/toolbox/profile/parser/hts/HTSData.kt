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

import java.util.Calendar

/**
 * HTS data class that holds the temperature data.
 *
 * @param temperature The temperature value.
 * @param unit The unit of the temperature value.
 * @param timestamp The timestamp of the measurement.
 * @param type The type of the measurement.
 */
data class HTSData(
    val temperature: Float = 0f,
    val unit: TemperatureUnitData = TemperatureUnitData.CELSIUS,
    val timestamp: Calendar? = null,
    val type: HTSMeasurementType? = null
) {
    override fun toString() = "$temperature $unit"
}

/**
 * The temperature unit data class.
 */
enum class TemperatureUnitData {
    CELSIUS, FAHRENHEIT;

    companion object {
        fun create(flag: Int): TemperatureUnitData? = when (flag) {
            0 -> CELSIUS
            1 -> FAHRENHEIT
            else -> null
        }
    }

    override fun toString() = when (this) {
        CELSIUS -> "°C"
        FAHRENHEIT -> "°F"
    }
}

/**
 * HTS measurement type enum.
 *
 * @property value The integer value representing the measurement type.
 */
enum class HTSMeasurementType(val value: Int) {
    FUTURE_USE(0),
    ARMPIT(1),
    BODY(2),
    EAR_LOBE(3),
    FINGER(4),
    GASTROINTESTINAL(5),
    MOUTH(6),
    RECTUM(7),
    TOE(8),
    TYMPANIC(9);

    override fun toString() = when (this) {
        FUTURE_USE -> "Future Use"
        ARMPIT -> "Armpit"
        BODY -> "Body"
        EAR_LOBE -> "Ear Lobe"
        FINGER -> "Finger"
        GASTROINTESTINAL -> "Gastrointestinal"
        MOUTH -> "Mouth"
        RECTUM -> "Rectum"
        TOE -> "Toe"
        TYMPANIC -> "Tympanic (Ear Drum)"
    }

    companion object {
        fun fromValue(value: Int): HTSMeasurementType? = entries.find { it.value == value }
    }
}
