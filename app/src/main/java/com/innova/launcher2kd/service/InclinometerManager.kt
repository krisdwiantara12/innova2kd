package com.innova.launcher2kd.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Digital 3D Inclinometer Manager
 * Menghitung sudut tanjakan/turunan (Pitch °) dan kemiringan samping (Roll °)
 * menggunakan sensor gravitasi/akselerometer internal Headunit Concerto tanpa butuh OBD2.
 * Dilengkapi filter EMA untuk meredam getaran khas mesin diesel Innova 2KD.
 */
class InclinometerManager(
    context: Context,
    private val onInclineUpdate: (pitchDeg: Int, rollDeg: Int, isSteepWarning: Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var gravitySensor: Sensor? = null

    // Nilai filtered
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private val alpha = 0.12f // Low-pass filter damping getaran diesel

    init {
        gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun start() {
        gravitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return

        val ax = values[0]
        val ay = values[1]
        val az = values[2]

        // Perhitungan trigonometri sudut kemiringan dalam derajat
        // Sumbu orientasi headunit landscape (10 inci):
        // Roll: kemiringan ke kiri / kanan (rotasi sumbu Y)
        // Pitch: tanjakan / turunan depan-belakang (rotasi sumbu X/Z)
        val radToDeg = 180.0 / Math.PI
        val rawPitch = (atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble())) * radToDeg).toFloat()
        val rawRoll = (atan2(-ax.toDouble(), az.toDouble()) * radToDeg).toFloat()

        // Filter peredam getaran
        smoothedPitch = smoothedPitch + alpha * (rawPitch - smoothedPitch)
        smoothedRoll = smoothedRoll + alpha * (rawRoll - smoothedRoll)

        val pitchInt = Math.round(smoothedPitch).toInt()
        val rollInt = Math.round(smoothedRoll).toInt()

        // Peringatan jika tanjakan/turunan > 20° atau miring samping > 18°
        val isSteep = Math.abs(pitchInt) >= 20 || Math.abs(rollInt) >= 18

        onInclineUpdate(pitchInt, rollInt, isSteep)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
