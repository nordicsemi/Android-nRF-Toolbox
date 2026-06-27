package no.nordicsemi.android.toolbox.profile.parser.gls.data

enum class Carbohydrate(internal val value: Int) {
    RESERVED(0),
    BREAKFAST(1),
    LUNCH(2),
    DINNER(3),
    SNACK(4),
    DRINK(5),
    SUPPER(6),
    BRUNCH(7);

    companion object {
        fun create(value: Int): Carbohydrate = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Cannot create Carbohydrate for value $value")
    }
}