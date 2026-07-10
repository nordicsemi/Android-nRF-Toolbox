package no.nordicsemi.android.toolbox.profile.data

import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCFeatureData
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSData
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSSettingsUnit
import no.nordicsemi.android.toolbox.lib.utils.Profile

data class RSCSServiceData(
    override val profile: Profile = Profile.RSCS,
    val data: RSCSData = RSCSData(),
    val unit: RSCSSettingsUnit = RSCSSettingsUnit.UNIT_METRIC,
    val feature: RSCFeatureData? = null,
) : ProfileServiceData()
