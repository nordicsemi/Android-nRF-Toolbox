package no.nordicsemi.android.toolbox.profile.parser.csc

enum class SpeedUnit(val displayName: String, val unit: String) {
    M_S("Meter per second", "m/s"),
    KM_H("Kilometer per hour", "km/h"),
    MPH("Mile per hour", "mph")
}