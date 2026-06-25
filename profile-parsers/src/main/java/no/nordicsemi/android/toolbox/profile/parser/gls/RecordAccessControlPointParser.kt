package no.nordicsemi.android.toolbox.profile.parser.gls

import no.nordicsemi.android.toolbox.profile.parser.gls.data.NumberOfRecordsData
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RecordAccessControlPointData
import no.nordicsemi.android.toolbox.profile.parser.gls.data.ResponseData
import no.nordicsemi.android.toolbox.profile.parser.racp.RACPOpCode
import no.nordicsemi.android.toolbox.profile.parser.racp.RACPResponseCode
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder

object RecordAccessControlPointParser {
    private const val OP_CODE_NUMBER_OF_STORED_RECORDS_RESPONSE = 5
    private const val OP_CODE_RESPONSE_CODE = 6
    private const val OPERATOR_NULL = 0

    fun parse(
        data: ByteArray,
        byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): RecordAccessControlPointData? {

        if (data.size < 3) return null

        val opCode: Int = data.getInt(0, IntFormat.UINT8)
        if (opCode != OP_CODE_NUMBER_OF_STORED_RECORDS_RESPONSE && opCode != OP_CODE_RESPONSE_CODE) {
            return null
        }

        val operator: Int = data.getInt(1, IntFormat.UINT8)
        if (operator != OPERATOR_NULL) return null

        when (opCode) {
            OP_CODE_NUMBER_OF_STORED_RECORDS_RESPONSE -> {
                // Field size is defined per service
                val numberOfRecords: Int = when (data.size - 2) {
                    1 -> data.getInt(2, IntFormat.UINT8)
                    2 -> data.getInt(2, IntFormat.UINT16, byteOrder)
                    4 -> data.getInt(2, IntFormat.UINT32, byteOrder)
                    else -> {
                        // Other field sizes are not supported
                        return null
                    }
                }
                return NumberOfRecordsData(numberOfRecords)
            }

            OP_CODE_RESPONSE_CODE -> {
                if (data.size != 4) return null

                val requestCode: Int = data.getInt(2, IntFormat.UINT8)
                val racpOpCode = RACPOpCode.create(requestCode)
                val responseCode: Int = data.getInt(3, IntFormat.UINT8)
                val racpResponseCode = RACPResponseCode.create(responseCode)
                return ResponseData(racpOpCode, racpResponseCode)
            }
        }
        return null
    }
}