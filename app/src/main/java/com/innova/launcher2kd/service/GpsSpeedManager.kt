package com.innova.launcher2kd.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlin.math.roundToInt

class GpsSpeedManager(
    private val context: Context,
    private val onSpeedUpdate: (speedKmH: Int, altitudeM: Int, heading: String, bearingDeg: Int) -> Unit,
    private val onDistanceUpdate: (deltaKm: Double) -> Unit,
    private val onGpsStatusChanged: (isLocked: Boolean) -> Unit
) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var lastLocation: Location? = null
    private var filteredSpeed = 0f
    private var lastSpeedTimestamp = 0L

    // Low-pass filter smoothing factor (0.0 to 1.0)
    private val alpha = 0.35f

    // Inertial dead-reckoning window: 3000 ms
    private val tunnelHoldMs = 3000L
    private var isStarted = false

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isStarted) return
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    800L, // 800ms for smooth 1Hz+ updates
                    0f,
                    this
                )
                isStarted = true
                onGpsStatusChanged(false)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        if (!isStarted) return
        locationManager.removeUpdates(this)
        isStarted = false
    }

    override fun onLocationChanged(location: Location) {
        onGpsStatusChanged(true)
        lastSpeedTimestamp = System.currentTimeMillis()

        // 1. Calculate raw speed in km/h
        val rawSpeedKmH = if (location.hasSpeed()) {
            location.speed * 3.6f
        } else {
            calculateSpeedFromDistance(location)
        }

        // 2. Low-Pass Filter smoothing (Anti-Jitter)
        filteredSpeed = (alpha * rawSpeedKmH) + ((1 - alpha) * filteredSpeed)
        if (filteredSpeed < 1.5f) filteredSpeed = 0f // Deadband threshold for car at standstill

        // 3. Accumulate distance
        lastLocation?.let { prev ->
            val distMeters = prev.distanceTo(location)
            if (distMeters in 1.0..100.0) { // filter out GPS jumps
                onDistanceUpdate(distMeters / 1000.0)
            }
        }
        lastLocation = location

        // 4. Altitude & Heading
        val altitudeM = if (location.hasAltitude()) location.altitude.roundToInt() else 0
        val bearingDeg = if (location.hasBearing()) location.bearing.roundToInt() else 0
        val headingText = degreeToCardinal(bearingDeg)

        onSpeedUpdate(filteredSpeed.roundToInt(), altitudeM, headingText, bearingDeg)
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

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            onGpsStatusChanged(false)
        }
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            onGpsStatusChanged(false)
            onSpeedUpdate(0, 0, "STANDSTILL", 0)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
