package no.nordicsemi.android.toolbox.profile.parser.cgms.data

data class CGMRecord(
    val glucoseConcentration: Float,
    val trend: Float?,
    val quality: Float?,
    val status: CGMStatus?,
    val timeOffset: Int,
    val crcPresent: Boolean
) {
    override fun toString() = buildString {
        append(glucoseConcentration)
        append(" mg/dL, time offset: ")
        append(timeOffset)
        append(" min, ")
        if (trend != null) append("Trend: $trend mg/dL/min, ")
        if (quality != null) append("Quality: $quality%, ")
        if (status != null) append("Status: $status, ")
    }.removeSuffix(", ")
}
