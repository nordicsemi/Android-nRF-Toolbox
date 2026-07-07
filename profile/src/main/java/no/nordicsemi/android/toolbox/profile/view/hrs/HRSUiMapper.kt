package no.nordicsemi.android.toolbox.profile.view.hrs

import no.nordicsemi.android.toolbox.profile.data.HRSServiceData

fun HRSServiceData.displayHeartRate(): String {
    return "${this.heartRate} BPM"
}