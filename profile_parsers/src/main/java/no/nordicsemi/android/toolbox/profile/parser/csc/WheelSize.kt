package no.nordicsemi.android.toolbox.profile.parser.csc

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
        // Those were ChatGPT-generated. If incorrect, please update.
        WheelSize(2201, "60-584", "27.5×2.35 / 650B"),
        WheelSize(2166, "57-584", "27.5×2.25 / 650B"),
        WheelSize(2132, "54-584", "27.5×2.1 / 650B"),
        WheelSize(2089, "50-584", "27.5×2.0 / 650B"),
        WheelSize(2055, "47-584", "27.5×1.75 / 650B"),

        WheelSize(2071, "37-590", "27×1⅜ (Vintage Road)"),

        WheelSize(2150, "60-559", "26×2.35 (26\" MTB)"),
        WheelSize(2123, "57-559", "26×2.25 (26\" MTB)"),
        WheelSize(2097, "54-559", "26×2.1 (26\" MTB)"),
        WheelSize(2070, "50-559", "26×2.0 (26\" MTB)"),
        WheelSize(2051, "47-559", "26×1.75 (26\" MTB)"),

        WheelSize(2243, "45-622", "700×45c (Adventure/Gravel)"),
        WheelSize(2192, "40-622", "700×40c (Gravel)"),
        WheelSize(2169, "38-622", "700×38c (City/Gravel)"),
        WheelSize(2139, "35-622", "700×35c (Hybrid/Gravel)"),
        WheelSize(2108, "32-622", "700×32c (Commuter/Gravel)"),
        WheelSize(2076, "28-622", "700×28c (All-Road)"),
        WheelSize(2058, "25-622", "700×25c (Road Endurance)"),
        WheelSize(2045, "23-622", "700×23c (Road Racing)"),

        WheelSize(1888, "47-507", "24×1.75 (Kids/Junior MTB)"),

        WheelSize(1634, "57-406", "20×2.125 (BMX/Folding)"),
        WheelSize(1571, "47-406", "20×1.75 (BMX/Folding)"),
    )
    val default = data.first()

    fun getWheelSizeByName(name: String) = data.find { it.name == name } ?: default
}
