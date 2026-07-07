package no.nordicsemi.android.toolbox.profile.parser.hrs

enum class BodySensorLocation(internal val value: Int) {
    OTHER(0),
    CHEST(1),
    WRIST(2),
    FINGER(3),
    HAND(4),
    EAR_LOBE(5),
    FOOT(6),
    RESERVED(7);

    companion object {
        fun create(value: Int): BodySensorLocation = entries.firstOrNull { it.value == value } ?: RESERVED
    }

    override fun toString() = when (this) {
        OTHER -> "Other"
        CHEST -> "Chest"
        WRIST -> "Wrist"
        FINGER -> "Finger"
        HAND -> "Hand"
        EAR_LOBE -> "Ear Lobe"
        FOOT -> "Foot"
        RESERVED -> "Reserved"
    }
}