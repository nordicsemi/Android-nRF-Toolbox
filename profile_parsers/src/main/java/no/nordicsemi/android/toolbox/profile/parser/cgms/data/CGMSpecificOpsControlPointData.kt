package no.nordicsemi.android.toolbox.profile.parser.cgms.data

data class CGMSpecificOpsControlPointData(
    var isOperationCompleted: Boolean = false,
    val secured: Boolean = false,
    val crcValid: Boolean = false,
    val requestCode: CGMOpCode? = null,
    val errorCode: CGMErrorCode? = null,
    val glucoseCommunicationInterval: Int = 0,
    val glucoseConcentrationOfCalibration: Float = 0f,
    val calibrationTime: Int = 0,
    val nextCalibrationTime: Int = 0,
    val type: CGMType? = null,
    val sampleLocation: CGMLocation? = null,
    val calibrationDataRecordNumber: Int = 0,
    val calibrationStatus: CGMCalibrationStatus? = null,
    val alertLevel: Float = 0f
) {
    override fun toString(): String {
        if (secured && !crcValid) return "Invalid CRC"
        return buildString {
            if (requestCode != null) {
                append("Response for '$requestCode': ")
                if (errorCode != null) append(errorCode) else append("Success")
                append(", ")
            }
            if (glucoseCommunicationInterval > 0) append("Glucose communication interval: $glucoseCommunicationInterval min, ")
            if (glucoseConcentrationOfCalibration > 0) append("Glucose concentration of calibration: $glucoseConcentrationOfCalibration mg/dL, ")
            if (calibrationTime > 0) append("Calibration time: $calibrationTime min, ")
            if (nextCalibrationTime > 0) append("Next calibration time: $nextCalibrationTime min, ")
            if (type != null) append("Type: $type, ")
            if (sampleLocation != null) append("Sample location: $sampleLocation, ")
            if (calibrationDataRecordNumber > 0) append("Calibration data record number: $calibrationDataRecordNumber, ")
            if (calibrationStatus != null) append("Calibration status: $calibrationStatus, ")
            if (alertLevel > 0) append("Alert level: $alertLevel mg/dL/min, ")
        }.removeSuffix(", ")
    }
}
