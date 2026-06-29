package no.nordicsemi.android.toolbox.profile.parser.gls

import no.nordicsemi.android.toolbox.profile.parser.gls.data.Carbohydrate
import no.nordicsemi.android.toolbox.profile.parser.gls.data.GLSMeasurementContext
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Health
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Meal
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Medication
import no.nordicsemi.android.toolbox.profile.parser.gls.data.MedicationUnit
import no.nordicsemi.android.toolbox.profile.parser.gls.data.Tester
import no.nordicsemi.kotlin.data.FloatFormat
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getFloat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.ByteOrder

object GlucoseMeasurementContextParser {

    fun parse(data: ByteArray): GLSMeasurementContext? {
        if (data.size < 3) return null

        var offset = 0

        val flags: Int = data.getInt(offset++, IntFormat.UINT8)
        val carbohydratePresent = flags and 0x01 != 0
        val mealPresent = flags and 0x02 != 0
        val testerHealthPresent = flags and 0x04 != 0
        val exercisePresent = flags and 0x08 != 0
        val medicationPresent = flags and 0x10 != 0
        val medicationUnitLiter = (flags and 0x20) > 0
        val hbA1cPresent = flags and 0x40 != 0
        val extendedFlagsPresent = flags and 0x80 != 0

        if (data.size < (3 + (if (carbohydratePresent) 3 else 0) + (if (mealPresent) 1 else 0) + (if (testerHealthPresent) 1 else 0)
                    + (if (exercisePresent) 3 else 0) + (if (medicationPresent) 3 else 0) + (if (hbA1cPresent) 2 else 0)
                    + if (extendedFlagsPresent) 1 else 0)
        ) {
            return null
        }

        val sequenceNumber: Int = data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN)
        offset += 2

        // Optional fields
        if (extendedFlagsPresent) {
            // ignore extended flags
            offset += 1
        }

        var carbohydrate: Carbohydrate? = null
        var carbohydrateAmount: Float? = null

        if (carbohydratePresent) {
            val carbohydrateId: Int = data.getInt(offset, IntFormat.UINT8)
            carbohydrate = Carbohydrate.create(carbohydrateId)

            carbohydrateAmount =
                data.getFloat(offset + 1, FloatFormat.IEEE_11073_16_BIT, ByteOrder.LITTLE_ENDIAN) // in grams
            offset += 3
        }

        var meal: Meal? = null
        if (mealPresent) {
            val mealId: Int = data.getInt(offset, IntFormat.UINT8)
            meal = Meal.create(mealId)
            offset += 1
        }

        var tester: Tester? = null
        var health: Health? = null
        if (testerHealthPresent) {
            val testerAndHealth: Int = data.getInt(offset, IntFormat.UINT8)
            tester = Tester.create((testerAndHealth and 0xF0) shr 4)
            health = Health.create(testerAndHealth and 0x0F)
            offset += 1
        }

        var exerciseDuration: Int? = null
        var exerciseIntensity: Int? = null
        if (exercisePresent) {
            exerciseDuration = data.getInt(offset, IntFormat.UINT16, ByteOrder.LITTLE_ENDIAN) // in seconds
            exerciseIntensity = data.getInt(offset + 2, IntFormat.UINT8) // in percentage
            offset += 3
        }

        var medication: Medication? =
            null
        var medicationAmount: Float? = null
        var medicationUnit: MedicationUnit? = null
        if (medicationPresent) {
            val medicationId: Int = data.getInt(offset, IntFormat.UINT8)
            medication = Medication.create(medicationId)
            medicationAmount =
                data.getFloat(offset + 1, FloatFormat.IEEE_11073_16_BIT, ByteOrder.LITTLE_ENDIAN) // mg or ml
            medicationUnit =
                if (medicationUnitLiter) MedicationUnit.UNIT_LITER else MedicationUnit.UNIT_KG
            offset += 3
        }

        var hbA1c: Float? = null
        if (hbA1cPresent) {
            hbA1c = data.getFloat(offset, FloatFormat.IEEE_11073_16_BIT, ByteOrder.LITTLE_ENDIAN)
            // offset += 2;
        }

        return GLSMeasurementContext(
            sequenceNumber,
            carbohydrate,
            carbohydrateAmount,
            meal,
            tester,
            health,
            exerciseDuration,
            exerciseIntensity,
            medication,
            medicationAmount,
            medicationUnit,
            hbA1c
        )
    }
}