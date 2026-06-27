package no.nordicsemi.android.toolbox.profile.parser.gls.data

import no.nordicsemi.android.toolbox.profile.parser.racp.RACPOpCode
import no.nordicsemi.android.toolbox.profile.parser.racp.RACPResponseCode

sealed interface RecordAccessControlPointData {
    val operationCompleted: Boolean
}

data class NumberOfRecordsData(
    val numberOfRecords: Int
) : RecordAccessControlPointData {

    override val operationCompleted: Boolean = true
}

data class ResponseData(
    val requestCode: RACPOpCode,
    val responseCode: RACPResponseCode
) : RecordAccessControlPointData {

    override val operationCompleted: Boolean = when (responseCode) {
        RACPResponseCode.RACP_RESPONSE_SUCCESS,
        RACPResponseCode.RACP_ERROR_NO_RECORDS_FOUND -> true
        RACPResponseCode.RACP_ERROR_OP_CODE_NOT_SUPPORTED,
        RACPResponseCode.RACP_ERROR_INVALID_OPERATOR,
        RACPResponseCode.RACP_ERROR_OPERATOR_NOT_SUPPORTED,
        RACPResponseCode.RACP_ERROR_INVALID_OPERAND,
        RACPResponseCode.RACP_ERROR_ABORT_UNSUCCESSFUL,
        RACPResponseCode.RACP_ERROR_PROCEDURE_NOT_COMPLETED,
        RACPResponseCode.RACP_ERROR_OPERAND_NOT_SUPPORTED -> false
    }
}