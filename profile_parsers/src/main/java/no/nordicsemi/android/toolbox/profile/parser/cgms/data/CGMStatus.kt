package no.nordicsemi.android.toolbox.profile.parser.cgms.data

data class CGMStatusEnvelope(
    val status: CGMStatus,
    val timeOffset: Int,
    val secured: Boolean,
    val crcValid: Boolean
)

data class CGMStatus(
    val sessionStopped: Boolean,
    val deviceBatteryLow: Boolean,
    val sensorTypeIncorrectForDevice: Boolean,
    val sensorMalfunction: Boolean,
    val deviceSpecificAlert: Boolean,
    val generalDeviceFault: Boolean,
    val timeSyncRequired: Boolean,
    val calibrationNotAllowed: Boolean,
    val calibrationRecommended: Boolean,
    val calibrationRequired: Boolean,
    val sensorTemperatureTooHigh: Boolean,
    val sensorTemperatureTooLow: Boolean,
    val sensorResultLowerThenPatientLowLevel: Boolean,
    val sensorResultHigherThenPatientHighLevel: Boolean,
    val sensorResultLowerThenHypoLevel: Boolean,
    val sensorResultHigherThenHyperLevel: Boolean,
    val sensorRateOfDecreaseExceeded: Boolean,
    val sensorRateOfIncreaseExceeded: Boolean,
    val sensorResultLowerThenDeviceCanProcess: Boolean,
    val sensorResultHigherThenDeviceCanProcess: Boolean
) {

    constructor(warningStatus: Int, calibrationTempStatus: Int, sensorStatus: Int) : this(
        sessionStopped = warningStatus and 0x01 != 0,
        deviceBatteryLow = warningStatus and 0x02 != 0,
        sensorTypeIncorrectForDevice = warningStatus and 0x04 != 0,
        sensorMalfunction = warningStatus and 0x08 != 0,
        deviceSpecificAlert = warningStatus and 0x10 != 0,
        generalDeviceFault = warningStatus and 0x20 != 0,
        timeSyncRequired = calibrationTempStatus and 0x01 != 0,
        calibrationNotAllowed = calibrationTempStatus and 0x02 != 0,
        calibrationRecommended = calibrationTempStatus and 0x04 != 0,
        calibrationRequired = calibrationTempStatus and 0x08 != 0,
        sensorTemperatureTooHigh = calibrationTempStatus and 0x10 != 0,
        sensorTemperatureTooLow = calibrationTempStatus and 0x20 != 0,
        sensorResultLowerThenPatientLowLevel = sensorStatus and 0x01 != 0,
        sensorResultHigherThenPatientHighLevel = sensorStatus and 0x02 != 0,
        sensorResultLowerThenHypoLevel = sensorStatus and 0x04 != 0,
        sensorResultHigherThenHyperLevel = sensorStatus and 0x08 != 0,
        sensorRateOfDecreaseExceeded = sensorStatus and 0x10 != 0,
        sensorRateOfIncreaseExceeded = sensorStatus and 0x20 != 0,
        sensorResultLowerThenDeviceCanProcess = sensorStatus and 0x40 != 0,
        sensorResultHigherThenDeviceCanProcess = sensorStatus and 0x80 != 0
    )
}
