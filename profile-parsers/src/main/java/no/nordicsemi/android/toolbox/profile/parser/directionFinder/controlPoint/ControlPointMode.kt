package no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint

enum class ControlPointMode(val value: Int) {
    RTT(0x00),
    MCPD(0x01);

    companion object {
        fun create(value: Int): ControlPointMode = entries.find { it.value == value }
            ?: throw IllegalArgumentException("Cannot create Carbohydrate for value $value")
    }
}

enum class ControlPointRequestCode(val value: Int) {
    CHANGE_MODE(0x01),
    CHECK_MODE(0x0A);

    companion object {
        fun create(value: Int): ControlPointRequestCode? {
            return entries.find { it.value == value }
        }
    }
}

enum class ControlPointResponseCodeValue(val value: Int) {
    SUCCESS(0x01),
    OP_CODE_NOT_SUPPORTED(0x02),
    INVALID(0x03),
    FAILED(0x04);

    companion object {
        fun create(value: Int): ControlPointResponseCodeValue? {
            return entries.find { it.value == value }
        }
    }
}