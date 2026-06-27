package no.nordicsemi.android.toolbox.profile.parser.rscs

/**
 * Parses the RSCS Feature data from a byte array.
 *
 * @return An instance of [RSCFeatureData] if the data is valid, null otherwise.
 */
object RSCSFeatureDataParser {

    fun parse(data: ByteArray): RSCFeatureData? {
        if (data.size != 2) return null

        val featureFlags = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
        return RSCFeatureData(
            instantaneousStrideLengthMeasurementSupported = (featureFlags and 0x01) != 0,
            totalDistanceMeasurementSupported = (featureFlags and 0x02) != 0,
            walkingOrRunningStatusSupported = (featureFlags and 0x04) != 0,
            calibrationSupported = (featureFlags and 0x08) != 0,
            multipleSensorLocationsSupported = (featureFlags and 0x10) != 0
        )
    }

}
