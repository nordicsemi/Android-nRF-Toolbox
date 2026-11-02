package no.nordicsemi.android.toolbox.profile.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/**
 * Vibrates the phone: a short, light vibration for a click, and a longer one for a long click.
 *
 * Requires the VIBRATE permission.
 */
internal fun Context.vibrate(longClick: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        vibrateApi31(longClick)
    } else {
        vibrateLegacy(longClick)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Context.vibrateApi31(longClick: Boolean) {
    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    val attributes = VibrationAttributes.Builder()
        .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
        .build()
    val effect = if (longClick) {
        VibrationEffect.createOneShot(400L, VibrationEffect.DEFAULT_AMPLITUDE)
    } else {
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    }
    vm.vibrate(CombinedVibration.createParallel(effect), attributes)
}

private fun Context.vibrateLegacy(longClick: Boolean) {
    val durationMs = if (longClick) 400L else 50L
    @Suppress("DEPRECATION")
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        @Suppress("DEPRECATION")
        vibrator.vibrate(effect, audioAttributes)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
    }
}
