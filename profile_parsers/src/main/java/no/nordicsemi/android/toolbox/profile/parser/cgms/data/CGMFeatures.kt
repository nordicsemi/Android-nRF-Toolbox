package no.nordicsemi.android.toolbox.profile.parser.cgms.data

data class CGMFeaturesEnvelope(
    val features: CGMFeatures,
    val type: Int,
    val sampleLocation: Int,
    val secured: Boolean,
    val crcValid: Boolean
)

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
}
