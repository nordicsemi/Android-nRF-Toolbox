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
    val type: Int = 0,
    val sampleLocation: Int = 0,
    val calibrationDataRecordNumber: Int = 0,
    val calibrationStatus: CGMCalibrationStatus? = null,
    val alertLevel: Float = 0f
)
