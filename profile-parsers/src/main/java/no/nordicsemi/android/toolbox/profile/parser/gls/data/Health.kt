package no.nordicsemi.android.toolbox.profile.parser.gls.data

enum class Health(internal val value: Int) {
    RESERVED(0),
    MINOR_HEALTH_ISSUES(1),
    MAJOR_HEALTH_ISSUES(2),
    DURING_MENSES(3),
    UNDER_STRESS(4),
    NO_HEALTH_ISSUES(5),
    NOT_AVAILABLE(15);

    companion object {
        fun create(value: Int): Health = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Cannot create Health for value $value")
    }
}