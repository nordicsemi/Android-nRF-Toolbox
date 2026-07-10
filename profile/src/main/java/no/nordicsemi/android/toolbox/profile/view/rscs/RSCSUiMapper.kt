package no.nordicsemi.android.toolbox.profile.view.rscs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSData
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSSettingsUnit
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.toolbox.profile.data.RSCSServiceData
import java.util.Locale

@Composable
fun RSCSServiceData.displayActivity(): String =
    stringResource(id = if (data.running) R.string.rscs_running else R.string.rscs_walking)

@Composable
fun RSCSServiceData.displayPace(): String =
    stringResource(id = R.string.rscs_rpm, data.instantaneousCadence)

internal fun RSCSData.speedWithSpeedUnit(speedUnit: RSCSSettingsUnit): Float {
    return when (speedUnit) {
        RSCSSettingsUnit.UNIT_METRIC -> instantaneousSpeed
        RSCSSettingsUnit.UNIT_IMPERIAL -> instantaneousSpeed * 2.2369f
    }
}

internal fun RSCSServiceData.displaySpeed(): String? {
    val speedWithUnit = unit.let { data.speedWithSpeedUnit(it) }
    return when (unit) {
        RSCSSettingsUnit.UNIT_METRIC -> String.format(Locale.US, "%.1f m/s", speedWithUnit)
        RSCSSettingsUnit.UNIT_IMPERIAL -> String.format(Locale.US, "%.1f mph", speedWithUnit)
    }
}

/**
 * Returns the total distance in a formatted string based on the provided speed unit.
 *
 * @param speedUnit The unit to display the distance in.
 * @return A formatted string representing the total distance.
 */
internal fun RSCSData.displayDistance(speedUnit: RSCSSettingsUnit): String {
    if (totalDistance == null) return "-"
    return when (speedUnit) {
        RSCSSettingsUnit.UNIT_METRIC -> String.format(
            Locale.US,
            "%.0f m",
            totalDistance!!.toFloat()
        )

        RSCSSettingsUnit.UNIT_IMPERIAL -> String.format(
            Locale.US,
            "%.2f mile",
            totalDistance!!.toFloat().toMiles()
        )
    }
}

@Composable
internal fun RSCSServiceData.displayStrideLength(): String {
    if (data.strideLength == null) return "-"
    return when (unit) {
        RSCSSettingsUnit.UNIT_METRIC -> String.format(
            Locale.US,
            "%.2f m",
            data.strideLength!! / 100.0f
        )

        RSCSSettingsUnit.UNIT_IMPERIAL -> String.format(
            Locale.US,
            "%.2f ft",
            (data.strideLength!!.toFloat() / 100) * 3.28084f
        )
    }
}

private fun Float.toMiles(): Float {
    return this * 0.0006f
}