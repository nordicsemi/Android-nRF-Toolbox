package no.nordicsemi.android.toolbox.profile.parser.racp

enum class RACPOpCode(internal val value: Int) {
    RACP_OP_CODE_REPORT_STORED_RECORDS(1),
    RACP_OP_CODE_DELETE_STORED_RECORDS(2),
    RACP_OP_CODE_ABORT_OPERATION(3),
    RACP_OP_CODE_REPORT_NUMBER_OF_RECORDS(4);

    companion object {
        fun create(value: Int): RACPOpCode = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Cannot create RACP op code for value: $value")
    }

    override fun toString() = when (this) {
        RACP_OP_CODE_REPORT_STORED_RECORDS -> "Report stored records"
        RACP_OP_CODE_DELETE_STORED_RECORDS -> "Delete stored records"
        RACP_OP_CODE_ABORT_OPERATION -> "Abort operation"
        RACP_OP_CODE_REPORT_NUMBER_OF_RECORDS -> "Report number of records"
    }
}