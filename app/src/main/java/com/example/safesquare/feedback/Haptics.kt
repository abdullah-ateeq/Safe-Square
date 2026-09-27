package com.example.safesquare.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class Haptic { DICE, SAFE, CAPTURE, HOME, WIN }

class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun perform(haptic: Haptic) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            v.vibrate(
                when (haptic) {
                    Haptic.DICE -> 8L
                    Haptic.SAFE -> 15L
                    Haptic.CAPTURE -> 35L
                    Haptic.HOME -> 45L
                    Haptic.WIN -> 90L
                }
            )
            return
        }
        v.vibrate(effectFor(haptic))
    }

    private fun effectFor(haptic: Haptic): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (haptic) {
                Haptic.DICE -> return VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                Haptic.SAFE -> return VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                Haptic.CAPTURE -> return VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                Haptic.HOME -> return VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                Haptic.WIN -> Unit
            }
        }
        return when (haptic) {
            Haptic.DICE -> VibrationEffect.createOneShot(8, 50)
            Haptic.SAFE -> VibrationEffect.createOneShot(15, 110)
            Haptic.CAPTURE -> VibrationEffect.createOneShot(35, 200)
            Haptic.HOME -> VibrationEffect.createWaveform(longArrayOf(0, 20, 70, 30), intArrayOf(0, 150, 0, 220), -1)
            Haptic.WIN -> VibrationEffect.createWaveform(
                longArrayOf(0, 30, 80, 30, 80, 70),
                intArrayOf(0, 140, 0, 190, 0, 255),
                -1
            )
        }
    }
}
