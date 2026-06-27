package no.nordicsemi.android.toolbox.profile.parser.cgms.data

data class CGMRecord(
    val glucoseConcentration: Float,
    val trend: Float?,
    val quality: Float?,
    val status: CGMStatus?,
    val timeOffset: Int,
    val crcPresent: Boolean
)
