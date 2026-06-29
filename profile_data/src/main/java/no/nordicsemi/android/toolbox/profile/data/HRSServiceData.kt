package no.nordicsemi.android.toolbox.profile.data

import no.nordicsemi.android.toolbox.profile.parser.hrs.HRSData
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.parser.hrs.BodySensorLocation

/**
 * Heart Rate Service data.
 *
 * @param profile the profile.
 * @param data the list of heart rate data.
 * @param bodySensorLocation the body sensor location.
 * @param zoomIn true if the chart is zoomed in.
 */
data class HRSServiceData(
    override val profile: Profile = Profile.HRS,
    val heartRate: Int? = null,
    val data: List<HRSData> = emptyList(),
    val bodySensorLocation: BodySensorLocation? = null,
    val zoomIn: Boolean = false,
) : ProfileServiceData() {
    val heartRates = data.map { it.heartRate }
}
