package no.nordicsemi.android.toolbox.profile.parser.rscs

enum class RSCSSettingsUnit {
    UNIT_METRIC,
    UNIT_IMPERIAL;

    override fun toString(): String = when (this) {
        UNIT_METRIC -> "Metric"
        UNIT_IMPERIAL -> "Imperial"
    }
}
