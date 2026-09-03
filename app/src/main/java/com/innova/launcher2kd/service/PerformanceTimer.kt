package com.innova.launcher2kd.service

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * 0-60 & 0-100 KM/H Drag Acceleration Timer
 * Stopwatch sprint digital otomatis untuk menguji akselerasi Innova 2KD.
 * Otomatis standby saat mobil berhenti (0 KM/H) dan otomatis mulai saat mobil digas.
 */
class PerformanceTimer(
    context: Context,
    private val onTimerUpdate: (status: String, currentSec: Float, time0to60: Float, time0to100: Float) -> Unit
) {
    private val prefs = context.getSharedPreferences("performance_timer", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    private var state = "STANDBY" // STANDBY, READY, RUNNING, FINISHED
    private var startTimeMs = 0L
    private var time0to60 = prefs.getFloat("best_0_60", 0f)
    private var time0to100 = prefs.getFloat("best_0_100", 0f)

    private var currentRun0to60 = 0f
    private var currentRun0to100 = 0f

    private val ticker = object : Runnable {
        override fun run() {
            if (state == "RUNNING") {
                val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000f
                onTimerUpdate("SPRINT...", elapsedSec, currentRun0to60, currentRun0to100)
                handler.postDelayed(this, 100)
            }
        }
    }

    fun onSpeedUpdate(speedKmH: Int) {
        when (state) {
            "STANDBY", "FINISHED" -> {
                if (speedKmH == 0) {
                    state = "READY"
                    onTimerUpdate("SIAP DIGAS (0 KM/H)", 0f, time0to60, time0to100)
                }
            }
            "READY" -> {
                if (speedKmH > 2) {
                    state = "RUNNING"
                    startTimeMs = System.currentTimeMillis()
                    currentRun0to60 = 0f
                    currentRun0to100 = 0f
                    handler.post(ticker)
                }
            }
            "RUNNING" -> {
                val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000f
                if (speedKmH >= 60 && currentRun0to60 == 0f) {
                    currentRun0to60 = Math.round(elapsedSec * 100f) / 100f
                }
                if (speedKmH >= 100 && currentRun0to100 == 0f) {
                    currentRun0to100 = Math.round(elapsedSec * 100f) / 100f
                    state = "FINISHED"
                    handler.removeCallbacks(ticker)

                    // Update Rekor jika lebih cepat
                    if (time0to100 == 0f || currentRun0to100 < time0to100) {
                        time0to100 = currentRun0to100
                        prefs.edit().putFloat("best_0_100", time0to100).apply()
                    }
                    if (time0to60 == 0f || (currentRun0to60 > 0 && currentRun0to60 < time0to60)) {
                        time0to60 = currentRun0to60
                        prefs.edit().putFloat("best_0_60", time0to60).apply()
                    }

                    onTimerUpdate("SELESAI! (0-100: ${currentRun0to100}s)", currentRun0to100, currentRun0to60, currentRun0to100)
                }
                if (speedKmH == 0) {
                    state = "READY"
                    handler.removeCallbacks(ticker)
                    onTimerUpdate("SIAP DIGAS (0 KM/H)", 0f, time0to60, time0to100)
                }
            }
        }
    }

    fun resetRecords() {
        prefs.edit().remove("best_0_60").remove("best_0_100").apply()
        time0to60 = 0f
        time0to100 = 0f
        state = "STANDBY"
        onTimerUpdate("STANDBY", 0f, 0f, 0f)
    }

    fun getBest0to100(): Float = time0to100
}
