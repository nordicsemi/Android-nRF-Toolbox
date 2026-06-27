package no.nordicsemi.android.toolbox.profile.parser.gls

import androidx.annotation.IntRange
import java.nio.ByteBuffer

object RecordAccessControlPointInputParser {
    private const val OP_CODE_REPORT_STORED_RECORDS: Byte = 1
    private const val OP_CODE_DELETE_STORED_RECORDS: Byte = 2
    private const val OP_CODE_ABORT_OPERATION: Byte = 3
    private const val OP_CODE_REPORT_NUMBER_OF_RECORDS: Byte = 4
    private const val OP_CODE_NUMBER_OF_STORED_RECORDS_RESPONSE: Byte = 5
    private const val OP_CODE_RESPONSE_CODE: Byte = 6
    private const val OPERATOR_NULL: Byte = 0
    private const val OPERATOR_ALL_RECORDS: Byte = 1
    private const val OPERATOR_LESS_THEN_OR_EQUAL: Byte = 2
    private const val OPERATOR_GREATER_THEN_OR_EQUAL: Byte = 3
    private const val OPERATOR_WITHING_RANGE: Byte = 4
    private const val OPERATOR_FIRST_RECORD: Byte = 5
    private const val OPERATOR_LAST_RECORD: Byte = 6

    fun reportAllStoredRecords(): ByteArray {
        return create(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_ALL_RECORDS)
    }

    fun reportFirstStoredRecord(): ByteArray {
        return create(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_FIRST_RECORD)
    }

    fun reportLastStoredRecord(): ByteArray {
        return create(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_LAST_RECORD)
    }

    fun reportStoredRecordsLessThenOrEqualTo(
        filter: FilterType,
        parameter: Short
    ): ByteArray {
        return create(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_LESS_THEN_OR_EQUAL, filter, parameter)
    }

    fun reportStoredRecordsGreaterThenOrEqualTo(
        filter: FilterType, parameter: Short
    ): ByteArray {
        return create(
            OP_CODE_REPORT_STORED_RECORDS,
            OPERATOR_GREATER_THEN_OR_EQUAL,
            filter,
            parameter
        )
    }

    fun reportStoredRecordsFromRange(
        filter: FilterType,
        start: Short, end: Short
    ): ByteArray {
        return create(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_WITHING_RANGE, filter, start, end)
    }

    fun reportStoredRecordsLessThenOrEqualTo(@IntRange(from = 0) sequenceNumber: Short): ByteArray {
        return create(
            OP_CODE_REPORT_STORED_RECORDS,
            OPERATOR_LESS_THEN_OR_EQUAL,
            FilterType.SEQUENCE_NUMBER,
            sequenceNumber
        )
    }

    fun reportStoredRecordsGreaterThenOrEqualTo(@IntRange(from = 0) sequenceNumber: Short): ByteArray {
        return create(
            OP_CODE_REPORT_STORED_RECORDS,
            OPERATOR_GREATER_THEN_OR_EQUAL,
            FilterType.SEQUENCE_NUMBER,
            sequenceNumber
        )
    }

    fun reportStoredRecordsFromRange(
        @IntRange(from = 0) startSequenceNumber: Short,
        @IntRange(from = 0) endSequenceNumber: Short
    ): ByteArray {
        return create(
            OP_CODE_REPORT_STORED_RECORDS,
            OPERATOR_WITHING_RANGE,
            FilterType.SEQUENCE_NUMBER,
            startSequenceNumber,
            endSequenceNumber
        )
    }

    fun deleteAllStoredRecords(): ByteArray {
        return create(OP_CODE_DELETE_STORED_RECORDS, OPERATOR_ALL_RECORDS)
    }

    fun deleteFirstStoredRecord(): ByteArray {
        return create(OP_CODE_DELETE_STORED_RECORDS, OPERATOR_FIRST_RECORD)
    }

    fun deleteLastStoredRecord(): ByteArray {
        return create(OP_CODE_DELETE_STORED_RECORDS, OPERATOR_LAST_RECORD)
    }

    fun deleteStoredRecordsLessThenOrEqualTo(
        filter: FilterType,
        parameter: Short
    ): ByteArray {
        return create(OP_CODE_DELETE_STORED_RECORDS, OPERATOR_LESS_THEN_OR_EQUAL, filter, parameter)
    }

    fun deleteStoredRecordsGreaterThenOrEqualTo(
        filter: FilterType,
        parameter: Short
    ): ByteArray {
        return create(
            OP_CODE_DELETE_STORED_RECORDS,
            OPERATOR_GREATER_THEN_OR_EQUAL,
            filter,
            parameter
        )
    }

    fun deleteStoredRecordsFromRange(
        filter: FilterType,
        start: Short,
        end: Short
    ): ByteArray {
        return create(OP_CODE_DELETE_STORED_RECORDS, OPERATOR_WITHING_RANGE, filter, start, end)
    }

    fun deleteStoredRecordsLessThenOrEqualTo(@IntRange(from = 0) sequenceNumber: Short): ByteArray {
        return create(
            OP_CODE_DELETE_STORED_RECORDS,
            OPERATOR_LESS_THEN_OR_EQUAL,
            FilterType.SEQUENCE_NUMBER,
            sequenceNumber
        )
    }

    fun deleteStoredRecordsGreaterThenOrEqualTo(@IntRange(from = 0) sequenceNumber: Short): ByteArray {
        return create(
            OP_CODE_DELETE_STORED_RECORDS,
            OPERATOR_GREATER_THEN_OR_EQUAL,
            FilterType.SEQUENCE_NUMBER,
            sequenceNumber
        )
    }

    fun deleteStoredRecordsFromRange(
        @IntRange(from = 0) startSequenceNumber: Short,
        @IntRange(from = 0) endSequenceNumber: Short
    ): ByteArray {
        return create(
            OP_CODE_DELETE_STORED_RECORDS, OPERATOR_WITHING_RANGE,
            FilterType.SEQUENCE_NUMBER,
            startSequenceNumber, endSequenceNumber
        )
    }

    fun reportNumberOfAllStoredRecords(): ByteArray {
        return create(OP_CODE_REPORT_NUMBER_OF_RECORDS, OPERATOR_ALL_RECORDS)
    }

    fun reportNumberOfStoredRecordsLessThenOrEqualTo(
        filter: FilterType,
        parameter: Short
    ): ByteArray {
        return create(
            OP_CODE_REPORT_NUMBER_OF_RECORDS,
            OPERATOR_LESS_THEN_OR_EQUAL,
            filter,
            parameter
        )
    }

    fun reportNumberOfStoredRecordsGreaterThenOrEqualTo(
        filter: FilterType,
        parameter: Short
    ): ByteArray {
        return create(
            OP_CODE_REPORT_NUMBER_OF_RECORDS,
            OPERATOR_GREATER_THEN_OR_EQUAL,
            filter,
            parameter
        )
    }

    fun reportNumberOfStoredRecordsFromRange(
        filter: FilterType,
        start: Short,
        end: Short
    ): ByteArray {
        return create(OP_CODE_REPORT_NUMBER_OF_RECORDS, OPERATOR_WITHING_RANGE, filter, start, end)
    }

    fun reportNumberOfStoredRecordsLessThenOrEqualTo(@IntRange(from = 0) sequenceNumber: Short): ByteArray {
        return create(
            OP_CODE_REPORT_NUMBER_OF_RECORDS,
            OPERATOR_LESS_THEN_OR_EQUAL,
            FilterType.SEQUENCE_NUMBER,
            sequenceNumber
        )
    }

    fun reportNumberOfStoredRecordsGreaterThenOrEqualTo(@IntRange(from = 0) sequenceNumber: Short): ByteArray {
        return create(
            OP_CODE_REPORT_NUMBER_OF_RECORDS,
            OPERATOR_GREATER_THEN_OR_EQUAL,
            FilterType.SEQUENCE_NUMBER,
            sequenceNumber
        )
    }

    fun reportNumberOfStoredRecordsFromRange(
        @IntRange(from = 0) startSequenceNumber: Short,
        @IntRange(from = 0) endSequenceNumber: Short
    ): ByteArray {
        return create(
            OP_CODE_REPORT_NUMBER_OF_RECORDS,
            OPERATOR_WITHING_RANGE,
            FilterType.SEQUENCE_NUMBER,
            startSequenceNumber,
            endSequenceNumber
        )
    }

    fun abortOperation(): ByteArray {
        return create(OP_CODE_ABORT_OPERATION, OPERATOR_NULL)
    }

    private fun create(opCode: Byte, operator: Byte): ByteArray {
        return byteArrayOf(opCode, operator)
    }

    private fun create(
        opCode: Byte, operator: Byte,
        filter: FilterType,
        vararg parameters: Short
    ): ByteArray {
        val data = ByteArray(2 + 1 + parameters.size * 2)
        val buffer = ByteBuffer.wrap(data).put(opCode).put(operator)

        if (parameters.isNotEmpty()) {
            buffer.put(filter.type).putShort(parameters[0])
        }
        if (parameters.size == 2) {
            buffer.putShort(parameters[1])
        }
        return buffer.array()
    }

    enum class FilterType(type: Int) {
        TIME_OFFSET(0x01),

        /** Alias of [.TIME_OFFSET]  */
        SEQUENCE_NUMBER(0x01), USER_FACING_TIME(0x02);

        val type: Byte = type.toByte()
    }
}