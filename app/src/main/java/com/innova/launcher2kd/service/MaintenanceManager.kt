package com.innova.launcher2kd.service

import android.content.Context
import android.content.SharedPreferences

class MaintenanceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("innova_2kd_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASE_ODOMETER = "base_odometer"
        private const val KEY_ACCUMULATED_DISTANCE = "accumulated_distance"
        private const val KEY_LAST_OIL_KM = "last_oil_km"
        private const val KEY_LAST_FUEL_FILTER_KM = "last_fuel_filter_km"
        private const val KEY_ENGINE_HOURS_SECONDS = "engine_hours_sec"

        const val INTERVAL_OIL_KM = 5000
        const val INTERVAL_FUEL_FILTER_KM = 10000
    }

    private var sessionDistanceKm = 0.0
    private var sessionStartTime = System.currentTimeMillis()

    // 1. Odometer
    fun getTotalOdometerKm(): Int {
        val base = prefs.getInt(KEY_BASE_ODOMETER, 142350)
        val accumulated = prefs.getFloat(KEY_ACCUMULATED_DISTANCE, 0f)
        return (base + accumulated + sessionDistanceKm).toInt()
    }

    fun setBaseOdometerKm(km: Int) {
        prefs.edit().putInt(KEY_BASE_ODOMETER, km).putFloat(KEY_ACCUMULATED_DISTANCE, 0f).apply()
        sessionDistanceKm = 0.0
    }

    fun addDistance(deltaKm: Double) {
        sessionDistanceKm += deltaKm
        val currentAcc = prefs.getFloat(KEY_ACCUMULATED_DISTANCE, 0f)
        prefs.edit().putFloat(KEY_ACCUMULATED_DISTANCE, (currentAcc + deltaKm).toFloat()).apply()
    }

    fun getSessionDistanceKm(): Double = sessionDistanceKm

    // 2. Oil Service (5.000 KM)
    fun getOilRemainingKm(): Int {
        val total = getTotalOdometerKm()
        val lastChange = prefs.getInt(KEY_LAST_OIL_KM, total - 1800) // default 3200km remaining
        val driven = total - lastChange
        return (INTERVAL_OIL_KM - driven).coerceAtLeast(0)
    }

    fun resetOilService() {
        prefs.edit().putInt(KEY_LAST_OIL_KM, getTotalOdometerKm()).apply()
    }

    // 3. Fuel Filter Service (10.000 KM for Biosolar B35/B40)
    fun getFuelFilterRemainingKm(): Int {
        val total = getTotalOdometerKm()
        val lastChange = prefs.getInt(KEY_LAST_FUEL_FILTER_KM, total - 2600) // default 7400km remaining
        val driven = total - lastChange
        return (INTERVAL_FUEL_FILTER_KM - driven).coerceAtLeast(0)
    }

    fun resetFuelFilterService() {
        prefs.edit().putInt(KEY_LAST_FUEL_FILTER_KM, getTotalOdometerKm()).apply()
    }

    // 4. Engine Hours
    fun addEngineHours(seconds: Long) {
        val totalSec = prefs.getLong(KEY_ENGINE_HOURS_SECONDS, 511200L) // default 142 hours
        prefs.edit().putLong(KEY_ENGINE_HOURS_SECONDS, totalSec + seconds).apply()
    }

    fun getEngineHoursFormatted(): String {
        val sessionElapsedSec = (System.currentTimeMillis() - sessionStartTime) / 1000
        val totalSec = prefs.getLong(KEY_ENGINE_HOURS_SECONDS, 511200L) + sessionElapsedSec
        val hours = totalSec / 3600
        return "$hours Jam"
    }

    // 5. Fuel Consumption Estimate (Innova 2KD ~ 11.5 km/L avg)
    fun getEstimatedSolarLiters(): Double {
        return sessionDistanceKm / 11.5
    }
}
