package no.nordicsemi.android.toolbox.profile.parser.racp

enum class RACPResponseCode(internal val value: Int) {
    RACP_RESPONSE_SUCCESS(1),
    RACP_ERROR_OP_CODE_NOT_SUPPORTED(2),
    RACP_ERROR_INVALID_OPERATOR(3),
    RACP_ERROR_OPERATOR_NOT_SUPPORTED(4),
    RACP_ERROR_INVALID_OPERAND(5),
    RACP_ERROR_NO_RECORDS_FOUND(6),
    RACP_ERROR_ABORT_UNSUCCESSFUL(7),
    RACP_ERROR_PROCEDURE_NOT_COMPLETED(8),
    RACP_ERROR_OPERAND_NOT_SUPPORTED(9);

    companion object {
        fun create(value: Int): RACPResponseCode = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Cannot create RACP response code for value: $value")
    }
}