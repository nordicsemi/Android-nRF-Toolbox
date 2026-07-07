package no.nordicsemi.android.toolbox.profile.parser.bps

import java.util.Calendar

enum class BloodPressureType(internal val value: Int) {
    UNIT_MMHG(0),
    UNIT_KPA(1);

    override fun toString(): String = when (this) {
        UNIT_MMHG -> "mmHg"
        UNIT_KPA -> "kPa"
    }
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
) {
    override fun toString(): String = "$systolic/$diastolic $unit${if (pulseRate != null) " ($pulseRate bpm)" else ""}${if (status != null) " Status: $status" else ""}"
}

data class IntermediateCuffPressureData(
    val cuffPressure: Float,
    val unit: BloodPressureType,
    val pulseRate: Float? = null,
    val userID: Int? = null,
    val status: BPMStatus? = null,
    val calendar: Calendar? = null
) {
    override fun toString(): String = "$cuffPressure $unit${if (pulseRate != null) " ($pulseRate bpm)" else ""}${if (status != null) " Status: $status" else ""}"
}
