package no.nordicsemi.android.toolbox.profile.parser.cgms.data

enum class CGMOpCode(val value: Int) {
    SET_COMMUNICATION_INTERVAL(1),
    SET_CALIBRATION_VALUE(4),
    SET_PATIENT_HIGH_ALERT_LEVEL(7),
    SET_PATIENT_LOW_ALERT_LEVEL(10),
    SET_HYPO_ALERT_LEVEL(13),
    SET_HYPER_ALERT_LEVEL(16),
    SET_RATE_OF_DECREASE_ALERT_LEVEL(19),
    SET_RATE_OF_INCREASE_ALERT_LEVEL(22),
    RESET_DEVICE_SPECIFIC_ERROR(25),
    START_SESSION(26),
    STOP_SESSION(27);

    companion object {
        fun create(value: Int): CGMOpCode = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Cannot create op code for value: $value")
    }
}
