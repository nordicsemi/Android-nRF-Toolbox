package no.nordicsemi.android.nrftoolbox.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.service.profile.ServiceApi
import no.nordicsemi.android.toolbox.profile.manager.BPSManager
import no.nordicsemi.android.toolbox.profile.manager.BatteryManager
import no.nordicsemi.android.toolbox.profile.manager.CGMManager
import no.nordicsemi.android.toolbox.profile.manager.CSCManager
import no.nordicsemi.android.toolbox.profile.manager.GLSManager
import no.nordicsemi.android.toolbox.profile.manager.HRSManager
import no.nordicsemi.android.toolbox.profile.manager.HTSManager
import no.nordicsemi.android.toolbox.profile.manager.LBSManager
import no.nordicsemi.android.toolbox.profile.manager.RSCSManager
import no.nordicsemi.android.toolbox.profile.manager.ServiceManager
import no.nordicsemi.android.toolbox.profile.data.uiMapper.TemperatureUnit
import no.nordicsemi.android.toolbox.profile.parser.csc.SpeedUnit
import no.nordicsemi.android.toolbox.profile.parser.gls.data.ConcentrationUnit
import no.nordicsemi.android.toolbox.profile.parser.rscs.RSCSSettingsUnit
import java.util.Locale

/** The most important value of a service, shown in the footer of the device card. */
internal data class DeviceValue(
    val icon: ImageVector,
    val text: String,
)

/**
 * Returns the most important values of the services of the device, if they are known yet,
 * in the order of the services.
 */
@Composable
internal fun ServiceApi.DeviceData.values(): List<DeviceValue> =
    services.mapNotNull { manager -> key(manager.profile) { manager.value() } }

@Composable
private fun ServiceManager.value(): DeviceValue? = when (this) {
    is BatteryManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        data.batteryLevel?.let { DeviceValue(Icons.Default.BatteryFull, "$it%") }
    }
    is HRSManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        data.heartRate?.let { DeviceValue(Icons.Default.Favorite, "$it bpm") }
    }
    is HTSManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        data.data?.temperature?.let {
            val text = when (data.temperatureUnit) {
                TemperatureUnit.CELSIUS -> String.format(Locale.US, "%.1f °C", it)
                TemperatureUnit.FAHRENHEIT -> String.format(Locale.US, "%.1f °F", it * 1.8f + 32f)
                TemperatureUnit.KELVIN -> String.format(Locale.US, "%.1f K", it + 273.15f)
            }
            DeviceValue(Icons.Default.Thermostat, text)
        }
    }
    is LBSManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        if (data.data.buttonState) {
            DeviceValue(Icons.Default.RadioButtonChecked, "Pressed")
        } else {
            DeviceValue(Icons.Default.RadioButtonUnchecked, "Released")
        }
    }
    is GLSManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        data.records.keys.maxByOrNull { it.sequenceNumber }?.let { record ->
            val value = record.glucoseConcentration ?: return@let null
            val text = when (record.unit) {
                ConcentrationUnit.UNIT_MOLPL -> String.format(Locale.US, "%.1f mmol/L", value)
                // kg/L is converted to mg/dL.
                else -> String.format(Locale.US, "%.0f mg/dL", value * 100_000f)
            }
            DeviceValue(Icons.Default.Bloodtype, text)
        }
    }
    is CGMManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        data.records.maxByOrNull { it.sequenceNumber }?.let {
            DeviceValue(
                Icons.Default.Bloodtype,
                String.format(Locale.US, "%.0f mg/dL", it.record.glucoseConcentration)
            )
        }
    }
    is CSCManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        val speed = data.data.speed
        val text = when (data.speedUnit) {
            SpeedUnit.M_S -> String.format(Locale.US, "%.1f m/s", speed)
            SpeedUnit.KM_H -> String.format(Locale.US, "%.1f km/h", speed * 3.6f)
            SpeedUnit.MPH -> String.format(Locale.US, "%.1f mph", speed * 2.2369f)
        }
        DeviceValue(Icons.Default.Speed, text)
    }
    is RSCSManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        val speed = data.data.instantaneousSpeed
        val text = when (data.unit) {
            RSCSSettingsUnit.UNIT_METRIC -> String.format(Locale.US, "%.1f m/s", speed)
            RSCSSettingsUnit.UNIT_IMPERIAL -> String.format(Locale.US, "%.1f mph", speed * 2.2369f)
        }
        DeviceValue(Icons.Default.Speed, text)
    }
    is BPSManager -> {
        val data by repository.data.collectAsStateWithLifecycle()
        data.bloodPressureMeasurement?.let {
            DeviceValue(
                Icons.Default.MonitorHeart,
                String.format(Locale.US, "%.0f/%.0f %s", it.systolic, it.diastolic, it.unit)
            )
        }
    }
    else -> null
}
