package no.nordicsemi.android.toolbox.profile.parser.gls.data

class GlucoseStatus(
    val deviceBatteryLow: Boolean,
    val sensorMalfunction: Boolean,
    val sampleSizeInsufficient: Boolean,
    val stripInsertionError: Boolean,
    val stripTypeIncorrect: Boolean,
    val sensorResultLowerThenDeviceCanProcess: Boolean,
    val sensorResultHigherThenDeviceCanProcess: Boolean,
    val sensorTemperatureTooHigh: Boolean,
    val sensorTemperatureTooLow: Boolean,
    val sensorReadInterrupted: Boolean,
    val generalDeviceFault: Boolean,
    val timeFault: Boolean
) {

    constructor(value: Int) : this(
        deviceBatteryLow = value and 0x0001 != 0,
        sensorMalfunction = value and 0x0002 != 0,
        sampleSizeInsufficient = value and 0x0004 != 0,
        stripInsertionError = value and 0x0008 != 0,
        stripTypeIncorrect = value and 0x0010 != 0,
        sensorResultLowerThenDeviceCanProcess = value and 0x0020 != 0,
        sensorResultHigherThenDeviceCanProcess = value and 0x0040 != 0,
        sensorTemperatureTooHigh = value and 0x0080 != 0,
        sensorTemperatureTooLow = value and 0x0100 != 0,
        sensorReadInterrupted = value and 0x0200 != 0,
        generalDeviceFault = value and 0x0400 != 0,
        timeFault = value and 0x0800 != 0
    )

    override fun toString(): String {
        return "GlucoseStatus(deviceBatteryLow=$deviceBatteryLow, sensorMalfunction=$sensorMalfunction, " +
                "sampleSizeInsufficient=$sampleSizeInsufficient, stripInsertionError=$stripInsertionError, " +
                "stripTypeIncorrect=$stripTypeIncorrect, sensorResultLowerThenDeviceCanProcess=$sensorResultLowerThenDeviceCanProcess, " +
                "sensorResultHigherThenDeviceCanProcess=$sensorResultHigherThenDeviceCanProcess, sensorTemperatureTooHigh=$sensorTemperatureTooHigh, " +
                "sensorTemperatureTooLow=$sensorTemperatureTooLow, sensorReadInterrupted=$sensorReadInterrupted, " +
                "generalDeviceFault=$generalDeviceFault, timeFault=$timeFault)"
    }
}