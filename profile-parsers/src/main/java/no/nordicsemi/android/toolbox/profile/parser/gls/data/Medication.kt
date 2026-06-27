package no.nordicsemi.android.toolbox.profile.parser.gls.data

enum class Medication(internal val value: Int) {
    RESERVED(0),
    RAPID_ACTING_INSULIN(1),
    SHORT_ACTING_INSULIN(2),
    INTERMEDIATE_ACTING_INSULIN(3),
    LONG_ACTING_INSULIN(4),
    PRE_MIXED_INSULIN(5);

    companion object {
        fun create(value: Int): Medication = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Cannot create Medication for value $value")
    }
}