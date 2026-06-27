package no.nordicsemi.android.toolbox.profile.parser.hrs

/**
 * Heart Rate data.
 * @param heartRate the heart rate value.
 * @param sensorContact true if the sensor contact is supported.
 * @param energyExpanded the energy expanded in joules.
 * @param rrIntervals the RR intervals in milliseconds.
 */
data class HRSData(
    val heartRate: Int,
    val sensorContact: Boolean,
    val energyExpanded: Int?,
    val rrIntervals: List<Int>
)
