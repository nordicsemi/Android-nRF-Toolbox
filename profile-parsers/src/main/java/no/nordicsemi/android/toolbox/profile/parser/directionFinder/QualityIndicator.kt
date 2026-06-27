package no.nordicsemi.android.toolbox.profile.parser.directionFinder

enum class QualityIndicator(val id: Int) {
    GOOD(0),
    POOR(1),
    NOT_FOR_USE(2),
    NOT_SPECIFIED(3);

    companion object {
        fun create(id: Int): QualityIndicator = entries.find { it.id == id }
            ?: throw IllegalArgumentException("Cannot find QualityIndicator for specified id: $id")
    }
}