package no.nordicsemi.android.toolbox.profile.data

import no.nordicsemi.android.toolbox.lib.utils.Profile

/**
 * Device Information Service data class.
 *
 * Every field is optional, as devices may expose only a subset of the characteristics.
 *
 * @param profile The profile.
 */
data class DISServiceData(
    override val profile: Profile = Profile.DIS,
    val manufacturerName: String? = null,
    val modelNumber: String? = null,
    val serialNumber: String? = null,
    val hardwareRevision: String? = null,
    val firmwareRevision: String? = null,
    val softwareRevision: String? = null,
    val systemId: String? = null,
    val ieeeCertificationData: String? = null,
    val pnpId: String? = null,
) : ProfileServiceData()
