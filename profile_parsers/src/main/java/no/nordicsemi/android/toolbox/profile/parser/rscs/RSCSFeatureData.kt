package no.nordicsemi.android.toolbox.profile.parser.rscs

/**
 * Data class representing the feature data of the Running Speed and Cadence Sensor (RSCS).
 * @param instantaneousStrideLengthMeasurementSupported Indicates if instantaneous stride length measurement is supported.
 * @param totalDistanceMeasurementSupported Indicates if total distance measurement is supported.
 * @param walkingOrRunningStatusSupported Indicates if walking or running status is supported.
 * @param calibrationSupported Indicates if calibration is supported.
 * @param multipleSensorLocationsSupported Indicates if multiple sensor locations are supported.
 */
data class RSCFeatureData(
    val instantaneousStrideLengthMeasurementSupported: Boolean,
    val totalDistanceMeasurementSupported: Boolean,
    val walkingOrRunningStatusSupported: Boolean,
    val calibrationSupported: Boolean,
    val multipleSensorLocationsSupported: Boolean
) {
    override fun toString() = buildString {
        if (instantaneousStrideLengthMeasurementSupported) {
            append("Instantaneous stride length measurement supported, ")
        }
        if (totalDistanceMeasurementSupported) {
            append("Total distance measurement supported, ")
        }
        if (walkingOrRunningStatusSupported) {
            append("Walking or running status supported, ")
        }
        if (calibrationSupported) {
            append("Calibration supported, ")
        }
        if (multipleSensorLocationsSupported) {
            append("Multiple sensor locations supported")
        }
    }.removeSuffix(", ")
}
