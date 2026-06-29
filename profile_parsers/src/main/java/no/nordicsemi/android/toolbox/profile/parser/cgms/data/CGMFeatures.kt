package no.nordicsemi.android.toolbox.profile.parser.cgms.data

data class CGMFeaturesEnvelope(
    val features: CGMFeatures,
    val type: CGMType,
    val sampleLocation: CGMLocation,
    val secured: Boolean,
    val crcValid: Boolean,
) {
    override fun toString() = buildString {
        append(features.toString())
        append(",\nType: ")
        append(type)
        append(", Sample location: ")
        append(sampleLocation)
        if (secured) {
            append(" (")
            if (crcValid) append("CRC valid") else append("CRC invalid")
            append(")")
        }
    }
}

class CGMFeatures(
    val calibrationSupported: Boolean,
    val patientHighLowAlertsSupported: Boolean,
    val hypoAlertsSupported: Boolean,
    val hyperAlertsSupported: Boolean,
    val rateOfIncreaseDecreaseAlertsSupported: Boolean,
    val deviceSpecificAlertSupported: Boolean,
    val sensorMalfunctionDetectionSupported: Boolean,
    val sensorTempHighLowDetectionSupported: Boolean,
    val sensorResultHighLowSupported: Boolean,
    val lowBatteryDetectionSupported: Boolean,
    val sensorTypeErrorDetectionSupported: Boolean,
    val generalDeviceFaultSupported: Boolean,
    val e2eCrcSupported: Boolean,
    val multipleBondSupported: Boolean,
    val multipleSessionsSupported: Boolean,
    val cgmTrendInfoSupported: Boolean,
    val cgmQualityInfoSupported: Boolean
) {

    constructor(value: Int) : this(
        calibrationSupported = value and 0x000001 != 0,
        patientHighLowAlertsSupported = value and 0x000002 != 0,
        hypoAlertsSupported = value and 0x000004 != 0,
        hyperAlertsSupported = value and 0x000008 != 0,
        rateOfIncreaseDecreaseAlertsSupported = value and 0x000010 != 0,
        deviceSpecificAlertSupported = value and 0x000020 != 0,
        sensorMalfunctionDetectionSupported = value and 0x000040 != 0,
        sensorTempHighLowDetectionSupported = value and 0x000080 != 0,
        sensorResultHighLowSupported = value and 0x000100 != 0,
        lowBatteryDetectionSupported = value and 0x000200 != 0,
        sensorTypeErrorDetectionSupported = value and 0x000400 != 0,
        generalDeviceFaultSupported = value and 0x000800 != 0,
        e2eCrcSupported = value and 0x001000 != 0,
        multipleBondSupported = value and 0x002000 != 0,
        multipleSessionsSupported = value and 0x004000 != 0,
        cgmTrendInfoSupported = value and 0x008000 != 0,
        cgmQualityInfoSupported = value and 0x010000 != 0
    )

    override fun toString() = buildString {
        if (calibrationSupported) append("Calibration supported, ")
        if (patientHighLowAlertsSupported) append("Patient high/low alerts supported, ")
        if (hypoAlertsSupported) append("Hypo alerts supported, ")
        if (hyperAlertsSupported) append("Hyper alerts supported, ")
        if (rateOfIncreaseDecreaseAlertsSupported) append("Rate of increase/decrease alerts supported, ")
        if (deviceSpecificAlertSupported) append("Device specific alert supported, ")
        if (sensorMalfunctionDetectionSupported) append("Sensor malfunction detection supported, ")
        if (sensorTempHighLowDetectionSupported) append("Sensor temperature high/low detection supported, ")
        if (sensorResultHighLowSupported) append("Sensor result high/low supported, ")
        if (lowBatteryDetectionSupported) append("Low battery detection supported, ")
        if (sensorTypeErrorDetectionSupported) append("Sensor type error detection supported, ")
        if (generalDeviceFaultSupported) append("General device fault supported, ")
        if (e2eCrcSupported) append("E2E-CRC supported, ")
        if (multipleBondSupported) append("Multiple bond supported, ")
        if (multipleSessionsSupported) append("Multiple sessions supported, ")
        if (cgmTrendInfoSupported) append("CGM trend info supported, ")
        if (cgmQualityInfoSupported) append("CGM quality info supported, ")
    }.removeSuffix(", ").ifEmpty { "None" }
}

enum class CGMType(internal val value: Int) {
    RESERVED(0x0),
    CAPILLARY_WHOLE_BLOOD(0x1),
    CAPILLARY_PLASMA(0x2),
    VENOUS_WHOLE_BLOOD(0x3),
    VENOUS_PLASMA(0x4),
    ARTERIAL_WHOLE_BLOOD(0x5),
    ARTERIAL_PLASMA(0x6),
    UNDETERMINED_WHOLE_BLOOD(0x7),
    UNDETERMINED_PLASMA(0x8),
    INTERSTITIAL_FLUID(0x9),
    CONTROL_SOLUTION(0xA);

    companion object {
        fun create(value: Int) = CGMType.entries.firstOrNull { it.value == value } ?: RESERVED
    }

    override fun toString() = when (this) {
        CAPILLARY_WHOLE_BLOOD -> "Capillary whole blood"
        CAPILLARY_PLASMA -> "Capillary plasma"
        VENOUS_WHOLE_BLOOD -> "Venous whole blood"
        VENOUS_PLASMA -> "Venous plasma"
        ARTERIAL_WHOLE_BLOOD -> "Arterial whole blood"
        ARTERIAL_PLASMA -> "Arterial plasma"
        UNDETERMINED_WHOLE_BLOOD -> "Undetermined whole blood"
        UNDETERMINED_PLASMA -> "Undetermined plasma"
        INTERSTITIAL_FLUID -> "Interstitial fluid"
        CONTROL_SOLUTION -> "Control solution"
        RESERVED -> "Reserved"
    }
}

enum class CGMLocation(internal val value: Int) {
    RESERVED(0x0),
    FINGER(0x1),
    ALTERNATE_SITE_TEST(0x2),
    EARLOBE(0x3),
    CONTROL_SOLUTION(0x4),
    SUBCUTANEOUS_TISSUE(0x5),
    NOT_AVAILABLE(0xF);

    companion object {
        fun create(value: Int) = CGMLocation.entries.firstOrNull { it.value == value } ?: RESERVED
    }

    override fun toString() = when (this) {
        FINGER -> "Finger"
        ALTERNATE_SITE_TEST -> "Alternate site test"
        EARLOBE -> "Earlobe"
        CONTROL_SOLUTION -> "Control solution"
        SUBCUTANEOUS_TISSUE -> "Subcutaneous tissue"
        NOT_AVAILABLE -> "Not available"
        RESERVED -> "Reserved"
    }
}
