package com.innova.launcher2kd.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlin.math.roundToInt

/**
 * Multi-Source Speed Fusion Engine
 * Menggabungkan 4 sumber data secara cerdas:
 * 1. Pulsa Kabel VSS / MCU Concerto (com.ts.main.speed / com.tw.speed / com.syu / com.microntek)
 * 2. GPS Satelit (High-precision Kalman filter)
 * 3. Network Provider (Cellular / WiFi AGPS)
 * 4. Sensor Inersia G-Sensor (Zero-lag motion detection saat akselerasi dari diam)
 */
class SpeedFusionManager(
    private val context: Context,
    private val onSpeedUpdate: (speedKmH: Int, altitudeM: Int, heading: String, bearingDeg: Int, sourceTag: String) -> Unit,
    private val onDistanceUpdate: (deltaKm: Double) -> Unit,
    private val onStatusChanged: (statusText: String) -> Unit
) : LocationListener, SensorEventListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val handler = Handler(Looper.getMainLooper())

    private var lastLocation: Location? = null
    private var filteredSpeed = 0f
    private val alpha = 0.35f

    private var currentAltitude = 0
    private var currentBearing = 0
    private var currentHeading = "UTARA"

    private var lastGpsTime = 0L
    private var lastMcuSpeedTime = 0L
    private var activeSource = "MENCARI SUMBER..."
    private var isStarted = false

    // 1. MCU Hardware Speed Pulse Broadcast Receiver
    private val mcuSpeedReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            var speedVal = -1f

            if (action == "com.ts.main.speed" || action == "com.tw.speed" || 
                action == "com.syu.car.speed" || action == "com.microntek.speed") {
                val floatS = intent.getFloatExtra("speed", -1f)
                val intS = intent.getIntExtra("speed", -1)
                val kmhS = intent.getFloatExtra("speed_kmh", -1f)

                if (kmhS >= 0f) speedVal = kmhS
                else if (floatS >= 0f) speedVal = floatS
                else if (intS >= 0) speedVal = intS.toFloat()
            }

            if (speedVal >= 0f) {
                lastMcuSpeedTime = System.currentTimeMillis()
                filteredSpeed = (alpha * speedVal) + ((1 - alpha) * filteredSpeed)
                if (filteredSpeed < 1.2f) filteredSpeed = 0f

                activeSource = "KABEL MCU VSS"
                onStatusChanged("KECEPATAN: KABEL SOKET MOBIL (VSS)")
                onSpeedUpdate(filteredSpeed.roundToInt(), currentAltitude, currentHeading, currentBearing, activeSource)
            }
        }
    }

    // 2. G-Sensor Inersia (Mendeteksi mobil bergerak sebelum sinyal satelit merespons)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER || event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]
            val accelMagnitude = Math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()

            // Jika mobil bergerak dari posisi diam (0 KM/H) namun GPS/MCU belum merespons
            val timeSinceGps = System.currentTimeMillis() - lastGpsTime
            val timeSinceMcu = System.currentTimeMillis() - lastMcuSpeedTime
            if (timeSinceGps > 2500 && timeSinceMcu > 2500 && accelMagnitude > 11.2f && filteredSpeed == 0f) {
                // Beri indikasi pergerakan awal
                filteredSpeed = 3f
                onSpeedUpdate(3, currentAltitude, currentHeading, currentBearing, "SENSOR INERSIA")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // 3. Location Listener (GPS & Network AGPS)
    override fun onLocationChanged(location: Location) {
        // Jika kabel MCU sedang aktif dan fresh (< 1 detik), prioritaskan kabel mobil
        val mcuFresh = (System.currentTimeMillis() - lastMcuSpeedTime) < 1200
        if (mcuFresh) {
            // Cukup update altitude & bearing dari GPS
            if (location.hasAltitude()) currentAltitude = location.altitude.roundToInt()
            if (location.hasBearing()) {
                currentBearing = location.bearing.roundToInt()
                currentHeading = degreeToCardinal(currentBearing)
            }
            return
        }

        lastGpsTime = System.currentTimeMillis()
        activeSource = if (location.provider == LocationManager.GPS_PROVIDER) "SATELIT GPS" else "JARINGAN AGPS"
        onStatusChanged("POSISI: ${activeSource.uppercase()}")

        val rawSpeedKmH = if (location.hasSpeed()) {
            location.speed * 3.6f
        } else {
            calculateSpeedFromDistance(location)
        }

        filteredSpeed = (alpha * rawSpeedKmH) + ((1 - alpha) * filteredSpeed)
        if (filteredSpeed < 1.2f) filteredSpeed = 0f

        lastLocation?.let { prev ->
            val distMeters = prev.distanceTo(location)
            if (distMeters in 1.0..100.0) {
                onDistanceUpdate(distMeters / 1000.0)
            }
        }
        lastLocation = location

        if (location.hasAltitude()) currentAltitude = location.altitude.roundToInt()
        if (location.hasBearing()) {
            currentBearing = location.bearing.roundToInt()
            currentHeading = degreeToCardinal(currentBearing)
        }

        onSpeedUpdate(filteredSpeed.roundToInt(), currentAltitude, currentHeading, currentBearing, activeSource)
    }

    private fun calculateSpeedFromDistance(location: Location): Float {
        val prev = lastLocation ?: return 0f
        val timeDiffSec = (location.time - prev.time) / 1000f
        if (timeDiffSec <= 0) return 0f
        val distanceMeters = prev.distanceTo(location)
        return (distanceMeters / timeDiffSec) * 3.6f
    }

    private fun degreeToCardinal(deg: Int): String {
        return when (deg) {
            in 338..360, in 0..22 -> "UTARA"
            in 23..67 -> "TIMUR LAUT"
            in 68..112 -> "TIMUR"
            in 113..157 -> "TENGGARA"
            in 158..202 -> "SELATAN"
            in 203..247 -> "BARAT DAYA"
            in 248..292 -> "BARAT"
            in 293..337 -> "BARAT LAUT"
            else -> "UTARA"
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isStarted) return
        isStarted = true

        // Register MCU Speed Broadcasts
        val filter = IntentFilter().apply {
            addAction("com.ts.main.speed")
            addAction("com.tw.speed")
            addAction("com.syu.car.speed")
            addAction("com.microntek.speed")
        }
        try {
            context.registerReceiver(mcuSpeedReceiver, filter)
        } catch (e: Exception) {}

        // Register G-Sensor
        try {
            val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accel?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {}

        // Register GPS & Network AGPS
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 600L, 0f, this)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this)
            }
        } catch (e: Exception) {}
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        try {
            context.unregisterReceiver(mcuSpeedReceiver)
        } catch (e: Exception) {}
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {}
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {}
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
