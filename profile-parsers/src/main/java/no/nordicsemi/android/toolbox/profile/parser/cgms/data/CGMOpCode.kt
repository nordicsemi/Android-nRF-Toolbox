package no.nordicsemi.android.toolbox.profile.parser.cgms.data

enum class CGMOpCode(val value: Int) {
    CGM_OP_CODE_SET_COMMUNICATION_INTERVAL(1),
    CGM_OP_CODE_SET_CALIBRATION_VALUE(4),
    CGM_OP_CODE_SET_PATIENT_HIGH_ALERT_LEVEL(7),
    CGM_OP_CODE_SET_PATIENT_LOW_ALERT_LEVEL(10),
    CGM_OP_CODE_SET_HYPO_ALERT_LEVEL(13),
    CGM_OP_CODE_SET_HYPER_ALERT_LEVEL(16),
    CGM_OP_CODE_SET_RATE_OF_DECREASE_ALERT_LEVEL(19),
    CGM_OP_CODE_SET_RATE_OF_INCREASE_ALERT_LEVEL(22),
    CGM_OP_CODE_RESET_DEVICE_SPECIFIC_ERROR(25),
    CGM_OP_CODE_START_SESSION(26),
    CGM_OP_CODE_STOP_SESSION(27);

    companion object {
        fun create(value: Int): CGMOpCode = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Cannot create op code for value: $value")
    }
}
