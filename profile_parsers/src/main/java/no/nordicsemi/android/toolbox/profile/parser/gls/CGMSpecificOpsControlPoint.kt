package no.nordicsemi.android.toolbox.profile.parser.gls

import no.nordicsemi.android.toolbox.profile.parser.common.CRC16
import java.nio.ByteBuffer

/*
* Copyright (c) 2018, Nordic Semiconductor
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
* documentation and/or other materials provided with the distribution.
*
* 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
* software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
* HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
* USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

@Suppress("unused")
object CGMSpecificOpsControlPointDataParser {
    private const val OP_CODE_SET_COMMUNICATION_INTERVAL: Byte = 1
    private const val OP_CODE_GET_COMMUNICATION_INTERVAL: Byte = 2
    private const val OP_CODE_SET_CALIBRATION_VALUE: Byte = 4
    private const val OP_CODE_GET_CALIBRATION_VALUE: Byte = 5
    private const val OP_CODE_SET_PATIENT_HIGH_ALERT_LEVEL: Byte = 7
    private const val OP_CODE_GET_PATIENT_HIGH_ALERT_LEVEL: Byte = 8
    private const val OP_CODE_SET_PATIENT_LOW_ALERT_LEVEL: Byte = 10
    private const val OP_CODE_GET_PATIENT_LOW_ALERT_LEVEL: Byte = 11
    private const val OP_CODE_SET_HYPO_ALERT_LEVEL: Byte = 13
    private const val OP_CODE_GET_HYPO_ALERT_LEVEL: Byte = 14
    private const val OP_CODE_SET_HYPER_ALERT_LEVEL: Byte = 16
    private const val OP_CODE_GET_HYPER_ALERT_LEVEL: Byte = 17
    private const val OP_CODE_SET_RATE_OF_DECREASE_ALERT_LEVEL: Byte = 19
    private const val OP_CODE_GET_RATE_OF_DECREASE_ALERT_LEVEL: Byte = 20
    private const val OP_CODE_SET_RATE_OF_INCREASE_ALERT_LEVEL: Byte = 22
    private const val OP_CODE_GET_RATE_OF_INCREASE_ALERT_LEVEL: Byte = 23
    private const val OP_CODE_RESET_DEVICE_SPECIFIC_ERROR: Byte = 25
    private const val OP_CODE_START_SESSION: Byte = 26
    private const val OP_CODE_STOP_SESSION: Byte = 27

    fun startSession(secure: Boolean): ByteArray {
        return create(
            OP_CODE_START_SESSION,
            secure
        )
    }

    private fun create(opCode: Byte, secure: Boolean): ByteArray {
        val data = ByteArray(1 + if (secure) 2 else 0)
        val buffer = ByteBuffer.wrap(data).put(opCode)
        if (secure) {
            val crc = CRC16.MCRF4XX(buffer.array(), 0, data.size).toShort()
            buffer.putShort(crc)
        }
        return buffer.array()
    }
}