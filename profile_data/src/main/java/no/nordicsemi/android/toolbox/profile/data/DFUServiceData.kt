package no.nordicsemi.android.toolbox.profile.data

import no.nordicsemi.android.toolbox.lib.utils.Profile

data class DFUServiceData(
    override val profile: Profile = Profile.DFU,
    val dfuAppName: DFUsAvailable? = null,
) : ProfileServiceData()

enum class DFUsAvailable {
    DFU_SERVICE,
    SMP_SERVICE,
    LEGACY_DFU_SERVICE,
    EXPERIMENTAL_BUTTONLESS_DFU_SERVICE,
}