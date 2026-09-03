package com.example.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AudioEngine {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            // Initialize with media stream type and comfortable volume level (85%)
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSound(type: String) {
        val tg = toneGenerator ?: return
        try {
            when (type.uppercase()) {
                "START" -> {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                }
                "PAUSE" -> {
                    tg.startTone(ToneGenerator.TONE_PROP_ACK, 100)
                }
                "RESUME" -> {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
                }
                "WARNING" -> {
                    tg.startTone(ToneGenerator.TONE_CDMA_PIP, 80)
                }
                "FINISHED" -> {
                    tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
                }
                "CANCELED" -> {
                    tg.startTone(ToneGenerator.TONE_PROP_NACK, 250)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrate(context: Context, type: String) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                when (type.uppercase()) {
                    "START" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(100)
                        }
                    }
                    "PAUSE" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(50)
                        }
                    }
                    "WARNING" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(80)
                        }
                    }
                    "FINISHED" -> {
                        val pattern = longArrayOf(0, 200, 100, 200)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(pattern, -1)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
