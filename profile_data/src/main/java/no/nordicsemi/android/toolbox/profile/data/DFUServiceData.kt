package no.nordicsemi.android.toolbox.profile.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.lib.utils.R

internal const val DFU_PACKAGE_NAME = "no.nordicsemi.android.dfu"
internal const val DFU_APP_LINK =
    "https://play.google.com/store/apps/details?id=no.nordicsemi.android.dfu"

internal const val SMP_PACKAGE_NAME = "no.nordicsemi.android.nrfconnectdevicemanager"
internal const val SMP_APP_LINK =
    "https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrfconnectdevicemanager"

data class DFUServiceData(
    override val profile: Profile = Profile.DFU,
    val dfuAppName: DFUsAvailable? = null,
) : ProfileServiceData()

enum class DFUsAvailable(
    val packageName: String,
    val appLink: String,
    @param:StringRes val appName: Int,
    @param:DrawableRes val appIcon: Int,
    @param:StringRes val appShortName: Int,
) {
    DFU_SERVICE(
        packageName = DFU_PACKAGE_NAME,
        appLink = DFU_APP_LINK,
        appName = R.string.dfu_app_name,
        appIcon = R.drawable.ic_dfu,
        appShortName = R.string.dfu_short_name,
    ),
    SMP_SERVICE(
        packageName = SMP_PACKAGE_NAME,
        appLink = SMP_APP_LINK,
        appName = R.string.smp_app_name,
        appIcon = R.drawable.ic_device_manager,
        appShortName = R.string.smp_short_name,
    ),
    MDS_SERVICE(
        packageName = SMP_PACKAGE_NAME,
        appLink = SMP_APP_LINK,
        appName = R.string.mds_app_name,
        appIcon = R.drawable.ic_device_manager,
        appShortName = R.string.mds_short_name,
    ),
    LEGACY_DFU_SERVICE(
        packageName = DFU_PACKAGE_NAME,
        appLink = DFU_APP_LINK,
        appName = R.string.legacy_dfu_app_name,
        appIcon = R.drawable.ic_dfu,
        appShortName = R.string.legacy_dfu_short_name,
    ),
    EXPERIMENTAL_BUTTONLESS_DFU_SERVICE(
        packageName = DFU_PACKAGE_NAME,
        appLink = DFU_APP_LINK,
        appName = R.string.buttonless_dfu_app_name,
        appIcon = R.drawable.ic_dfu,
        appShortName = R.string.buttonless_dfu_short_name,
    )
}