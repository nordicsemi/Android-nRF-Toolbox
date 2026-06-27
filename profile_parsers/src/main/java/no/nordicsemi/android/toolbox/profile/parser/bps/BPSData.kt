package no.nordicsemi.android.toolbox.profile.parser.bps

import java.util.Calendar

enum class BloodPressureType(internal val value: Int) {
    UNIT_MMHG(0),
    UNIT_KPA(1)
}

data class BloodPressureMeasurementData(
    val systolic: Float,
    val diastolic: Float,
    val meanArterialPressure: Float,
    val unit: BloodPressureType,
    val pulseRate: Float?,
    val userID: Int?,
    val status: BPMStatus?,
    val calendar: Calendar?
)

data class IntermediateCuffPressureData(
    val cuffPressure: Float,
    val unit: BloodPressureType,
    val pulseRate: Float? = null,
    val userID: Int? = null,
    val status: BPMStatus? = null,
    val calendar: Calendar? = null
)
