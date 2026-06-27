package no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt

class ControlPointDataParser {

    fun parse(data: ByteArray): ControlPointResult? {
        if (data.isEmpty()) return null

        var offset = 0
        val responseCode = data.getInt(offset++, IntFormat.UINT8)

        if (responseCode != 0x10) return null

        data.getInt(offset, IntFormat.UINT8)
            .let { ControlPointRequestCode.create(it) }
            ?.let { controlPointRequestCode ->
                val result = data.getInt(offset++, IntFormat.UINT8)
                    .let { ControlPointResponseCodeValue.create(it) }
                if (result == null) {
                    return@let
                }
                return when (controlPointRequestCode) {
                    ControlPointRequestCode.CHANGE_MODE -> {
                        onChangeModeResult(result)
                    }
                    ControlPointRequestCode.CHECK_MODE -> {
                        onCheckModeResult(
                            opCode = result,
                            modes = data.slice(offset until data.size)
                                .map { ControlPointMode.create(it.toInt()) })
                    }
                }
            }
        return null
    }

    private fun onChangeModeResult(
        opCode: ControlPointResponseCodeValue,
    ): ControlPointResult = when (opCode) {
        ControlPointResponseCodeValue.SUCCESS -> ControlPointChangeModeSuccess
        ControlPointResponseCodeValue.OP_CODE_NOT_SUPPORTED,
        ControlPointResponseCodeValue.INVALID,
        ControlPointResponseCodeValue.FAILED -> ControlPointChangeModeError
    }

    private fun onCheckModeResult(
        opCode: ControlPointResponseCodeValue,
        modes: List<ControlPointMode>,
    ): ControlPointResult = when (opCode) {
        ControlPointResponseCodeValue.SUCCESS -> ControlPointCheckModeSuccess(modes)
        ControlPointResponseCodeValue.OP_CODE_NOT_SUPPORTED,
        ControlPointResponseCodeValue.INVALID,
        ControlPointResponseCodeValue.FAILED -> ControlPointCheckModeError
    }
}
