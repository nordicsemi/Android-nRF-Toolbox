package no.nordicsemi.android.toolbox.profile.parser.cgms.data

enum class CGMErrorCode(val value: Int) {
    CGM_ERROR_OP_CODE_NOT_SUPPORTED(2),
    CGM_ERROR_INVALID_OPERAND(3),
    CGM_ERROR_PROCEDURE_NOT_COMPLETED(4),
    CGM_ERROR_PARAMETER_OUT_OF_RANGE(5);

    companion object {
        fun create(value: Int): CGMErrorCode = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Cannot create error code for value: $value")
    }

    override fun toString() = when (this) {
        CGM_ERROR_OP_CODE_NOT_SUPPORTED -> "Not supported"
        CGM_ERROR_INVALID_OPERAND -> "Invalid operand"
        CGM_ERROR_PROCEDURE_NOT_COMPLETED -> "Procedure not completed"
        CGM_ERROR_PARAMETER_OUT_OF_RANGE -> "Parameter out of range"
    }
}
