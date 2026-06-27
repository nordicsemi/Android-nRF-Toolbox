package no.nordicsemi.android.toolbox.profile.parser.prx

enum class AlarmLevel(internal val value: Byte) {
    NONE(0x00),
    MEDIUM(0x01),
    HIGH(0x02);

    companion object {
        internal fun create(value: Int): AlarmLevel = entries.firstOrNull { it.value.toInt() == value }
            ?: throw IllegalArgumentException("Cannot find AlarmLevel for provided value: $value")
    }
}