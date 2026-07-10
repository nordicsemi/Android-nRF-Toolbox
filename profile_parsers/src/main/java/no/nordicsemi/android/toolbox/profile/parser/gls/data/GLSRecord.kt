package no.nordicsemi.android.toolbox.profile.parser.gls.data

import java.util.Calendar

data class GLSRecord(
    val sequenceNumber: Int,
    val time: Calendar? = null,
    val glucoseConcentration: Float? = null,
    val unit: ConcentrationUnit? = null,
    val type: RecordType? = null,
    val status: GlucoseStatus? = null,
    val sampleLocation: SampleLocation? = null,
    val contextInformationFollows: Boolean
) {
    override fun toString() = buildString {
        append(sequenceNumber)
        append(": ")
        if (glucoseConcentration != null) {
            append(glucoseConcentration)
            append(" ")
            append(unit)
        }
        if (type != null || sampleLocation != null) {
            append(" (")
            if (type != null) {
                append(type)
            }
            if (type != null && sampleLocation != null) {
                append(", ")
            }
            if (sampleLocation != null) {
                append(sampleLocation)
            }
            append(")")
        }
        if (status != null) {
            append(", status: ")
            append(status)
        }
    }
}

enum class RecordType(val id: Int) {
    CAPILLARY_WHOLE_BLOOD(1),
    CAPILLARY_PLASMA(2),
    VENOUS_WHOLE_BLOOD(3),
    VENOUS_PLASMA(4),
    ARTERIAL_WHOLE_BLOOD(5),
    ARTERIAL_PLASMA(6),
    UNDETERMINED_WHOLE_BLOOD(7),
    UNDETERMINED_PLASMA(8),
    INTERSTITIAL_FLUID(9),
    CONTROL_SOLUTION(10);

    companion object {
        fun create(value: Int): RecordType = entries.firstOrNull { it.id == value }
            ?: throw IllegalArgumentException("Cannot find element for provided value.")

        fun createOrNull(value: Int?): RecordType? {
            return entries.firstOrNull { it.id == value }
        }
    }

    override fun toString() = when (this) {
        CAPILLARY_WHOLE_BLOOD -> "Capillary Whole Blood"
        CAPILLARY_PLASMA -> "Capillary Plasma"
        VENOUS_WHOLE_BLOOD -> "Venous Whole Blood"
        VENOUS_PLASMA -> "Venous Plasma"
        ARTERIAL_WHOLE_BLOOD -> "Arterial Whole Blood"
        ARTERIAL_PLASMA -> "Arterial Plasma"
        UNDETERMINED_WHOLE_BLOOD -> "Undetermined Whole Blood"
        UNDETERMINED_PLASMA -> "Undetermined Plasma"
        INTERSTITIAL_FLUID -> "Interstitial Fluid"
        CONTROL_SOLUTION -> "Control Solution"
    }
}

data class GLSMeasurementContext(
    val sequenceNumber: Int = 0,
    val carbohydrate: Carbohydrate? = null,
    val carbohydrateAmount: Float? = null,
    val meal: Meal? = null,
    val tester: Tester? = null,
    val health: Health? = null,
    val exerciseDuration: Int? = null,
    val exerciseIntensity: Int? = null,
    val medication: Medication?,
    val medicationQuantity: Float? = null,
    val medicationUnit: MedicationUnit? = null,
    val HbA1c: Float? = null
)

enum class ConcentrationUnit(val id: Int) {
    UNIT_KGPL(1),
    UNIT_MOLPL(0);

    companion object {
        fun create(value: Int): ConcentrationUnit = entries.firstOrNull { it.id == value }
            ?: throw IllegalArgumentException("Cannot find element for provided value.")
    }

    override fun toString() = when (this) {
        UNIT_KGPL -> "kg/L"
        UNIT_MOLPL -> "mmol/L"
    }
}

enum class MedicationUnit(val id: Int) {
    UNIT_KG(0),
    UNIT_LITER(1);

    companion object {
        fun create(value: Int): MedicationUnit = entries.firstOrNull { it.id == value }
            ?: throw IllegalArgumentException("Cannot find element for provided value.")
    }

    override fun toString() = when (this) {
        UNIT_KG -> "kg"
        UNIT_LITER -> "L"
    }
}

enum class SampleLocation(val id: Int) {
    FINGER(1),
    AST(2),
    EARLOBE(3),
    CONTROL_SOLUTION(4),
    NOT_AVAILABLE(15);

    companion object {
        fun createOrNull(value: Int?): SampleLocation = entries.firstOrNull { it.id == value }
            ?: throw IllegalArgumentException("Cannot find element for provided value.")
    }

    override fun toString() = when (this) {
        FINGER -> "Finger"
        AST -> "AST"
        EARLOBE -> "Earlobe"
        CONTROL_SOLUTION -> "Control Solution"
        NOT_AVAILABLE -> "Not Available"
    }
}