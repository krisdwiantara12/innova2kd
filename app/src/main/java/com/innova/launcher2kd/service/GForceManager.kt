package com.innova.launcher2kd.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 3D G-Force Telemetry Manager
 * Menghitung gaya G akselerasi, deselerasi pengereman, dan gaya lateral menikung
 * dari sensor akselerometer linier / akselerometer internal.
 */
class GForceManager(
    context: Context,
    private val onGForceUpdate: (forwardG: Float, lateralG: Float, totalG: Float, maxG: Float) -> Unit
) : SensorEventListener {

    private val prefs = context.getSharedPreferences("gforce_telemetry", Context.MODE_PRIVATE)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var accelSensor: Sensor? = null

    private var maxGRecorded = prefs.getFloat("max_g_peak", 0f)
    private val gravityStandard = 9.80665f

    private var filteredForwardG = 0f
    private var filteredLateralG = 0f
    private val filterAlpha = 0.15f

    init {
        accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun start() {
        accelSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val vals = event?.values ?: return

        // ax = lateral (kiri-kanan)
        // ay = vertikal/dorong
        // az = depan-belakang
        val rawLatG = vals[0] / gravityStandard
        val rawFwdG = vals[2] / gravityStandard

        filteredLateralG += filterAlpha * (rawLatG - filteredLateralG)
        filteredForwardG += filterAlpha * (rawFwdG - filteredForwardG)

        val totalG = sqrt((filteredLateralG * filteredLateralG + filteredForwardG * filteredForwardG).toDouble()).toFloat()

        if (totalG > maxGRecorded) {
            maxGRecorded = Math.round(totalG * 100f) / 100f
            prefs.edit().putFloat("max_g_peak", maxGRecorded).apply()
        }

        val roundedFwd = Math.round(filteredForwardG * 100f) / 100f
        val roundedLat = Math.round(filteredLateralG * 100f) / 100f
        val roundedTot = Math.round(totalG * 100f) / 100f

        onGForceUpdate(roundedFwd, roundedLat, roundedTot, maxGRecorded)
    }

    fun resetPeakG() {
        maxGRecorded = 0f
        prefs.edit().putFloat("max_g_peak", 0f).apply()
        onGForceUpdate(0f, 0f, 0f, 0f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
