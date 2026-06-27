package no.nordicsemi.android.toolbox.profile.parser.common

enum class WorkingMode {
    ALL, FIRST, LAST;

    override fun toString(): String = when (this) {
        ALL -> "All records"
        LAST -> "Last record"
        FIRST -> "First record"
    }
}
