package no.nordicsemi.android.toolbox.profile.parser.csc

/**
 * @property value Wheel Circumference in millimeters.
 * @property name ISO code.
 * @property description Tire size.
 */
data class WheelSize(
    val value: Int,
    val name: String,
    val description: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WheelSize
        return value == other.value
    }

    override fun hashCode(): Int = value
}

object WheelSizes {
    val data = listOf(
        // Values based on Wahoo Fitness Tire Size Chart:
        // https://support.wahoofitness.com/hc/en-us/articles/26243161988882-Tire-Size-Wheel-Circumference-Chart
        WheelSize(2207, "60-584", "27.5×2.35 / 650B"),
        WheelSize(2182, "57-584", "27.5×2.25 / 650B"),
        WheelSize(2148, "54-584", "27.5×2.1 / 650B"),
        WheelSize(2090, "50-584", "27.5×1.95 / 650B"),
        WheelSize(2079, "40-584", "27.5×1.50 / 650B"),

        WheelSize(2068, "37-590", "26×1⅜ (Vintage Road)"),

        WheelSize(2083, "58-559", "26×2.35 (26\" MTB)"),
        WheelSize(2070, "57-559", "26×2.125 (26\" MTB)"),
        WheelSize(2068, "54-559", "26×2.1 (26\" MTB)"),
        WheelSize(2055, "52-559", "26×2.0 (26\" MTB)"),
        WheelSize(2023, "47-559", "26×1.75 (26\" MTB)"),

        WheelSize(2234, "45-622", "700×45c (Adventure/Gravel)"),
        WheelSize(2203, "40-622", "700×40c (Gravel)"),
        WheelSize(2190, "38-622", "700×38c (City/Gravel)"),
        WheelSize(2171, "35-622", "700×35c (Hybrid/Gravel)"),
        WheelSize(2152, "32-622", "700×32c (Commuter/Gravel)"),
        WheelSize(2127, "28-622", "700×28c (All-Road)"),
        WheelSize(2109, "25-622", "700×25c (Road Endurance)"),
        WheelSize(2096, "23-622", "700×23c (Road Racing)"),

        WheelSize(1890, "47-507", "24×1.75 (Kids/Junior MTB)"),

        WheelSize(1565, "50-406", "20×1.95 (BMX/Folding)"),
        WheelSize(1515, "47-406", "20×1.75 (BMX/Folding)"),
    )
    val default = data.first()

    fun getWheelSizeByName(name: String) = data.find { it.name == name } ?: default
}
