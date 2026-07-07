package no.nordicsemi.android.toolbox.profile.parser.bps

/**
 * Data class to hold the Blood Pressure Feature data.
 * @param bodyMovementDetection Indicates if body movement detection is supported.
 * @param cuffFitDetection Indicates if cuff fit detection is supported.
 * @param irregularPulseDetection Indicates if irregular pulse detection is supported.
 * @param pulseRateRangeDetection Indicates if pulse rate range detection is supported.
 * @param measurementPositionDetection Indicates if measurement position detection is supported.
 * @param multipleBonds Indicates if multiple bonds are supported.
 * @param e2eCrc Indicates if E2E CRC is supported.
 * @param userData Indicates if user data is supported.
 * @param userFacingTime Indicates if user facing time is supported.
 */
data class BloodPressureFeatureData(
    val bodyMovementDetection: Boolean,
    val cuffFitDetection: Boolean,
    val irregularPulseDetection: Boolean,
    val pulseRateRangeDetection: Boolean,
    val measurementPositionDetection: Boolean,
    val multipleBonds: Boolean,
    val e2eCrc: Boolean,
    val userData: Boolean,
    val userFacingTime: Boolean,
) {
    override fun toString(): String = buildString {
        if (bodyMovementDetection) append("Body Movement Detection, ")
        if (cuffFitDetection) append("Cuff Fit Detection, ")
        if (irregularPulseDetection) append("Irregular Pulse Detection, ")
        if (pulseRateRangeDetection) append("Pulse Rate Range Detection, ")
        if (measurementPositionDetection) append("Measurement Position Detection, ")
        if (multipleBonds) append("Multiple Bonds, ")
        if (e2eCrc) append("E2E CRC, ")
        if (userData) append("User Data, ")
        if (userFacingTime) append("User Facing Time")
    }.removeSuffix(", ")
}
