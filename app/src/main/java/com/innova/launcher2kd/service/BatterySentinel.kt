package com.innova.launcher2kd.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import java.io.File

/**
 * Concerto Multi-Source Precision Battery Engine
 * Dirancang khusus untuk Headunit Android 10" (Concerto / Topway / Allwinner / TS MCU Platform)
 * Membaca ADC Hardware, Multi-Broadcast Interception, dan EMA Precision Smoothing Filter.
 */
class BatterySentinel(
    private val context: Context,
    private val onVoltageUpdate: (voltage: Float, isCrankingDrop: Boolean, statusText: String) -> Unit
) {
    private var lowestCrankVoltage = 14.0f
    private var isMonitoringCrank = true
    private val monitorStartTime = System.currentTimeMillis()
    private var filteredVoltage = 12.6f

    private val handler = Handler(Looper.getMainLooper())
    private var isPollingSysfs = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            var rawCalculated = -1.0f

            // 1. Concerto / Topway / TS MCU Dedicated Broadcasts
            if (action == "com.tw.battery.voltage" || 
                action == "com.ts.main.battery" || 
                action == "com.microntek.battery" ||
                action == "com.forfan.action.BATTERY_VOLTAGE") {
                val floatV = intent.getFloatExtra("voltage", -1.0f)
                val intV = intent.getIntExtra("voltage", -1)
                val carV = intent.getFloatExtra("car_voltage", -1.0f)

                if (floatV > 8.0f) rawCalculated = floatV
                else if (carV > 8.0f) rawCalculated = carV
                else if (intV > 8000) rawCalculated = intV / 1000.0f
                else if (intV > 800) rawCalculated = intV / 100.0f
            }

            // 2. Standard Android Battery Intent (Concerto ADC Kernel Bridge)
            if (rawCalculated <= 0.0f && action == Intent.ACTION_BATTERY_CHANGED) {
                val rawMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                rawCalculated = parseRawMvToAutomotive(rawMv)
            }

            if (rawCalculated > 0.0f) {
                processVoltageReading(rawCalculated)
            }
        }
    }

    private fun parseRawMvToAutomotive(rawMv: Int): Float {
        return when {
            // Raw car millivolts directly from Concerto MCU (e.g. 12450 = 12.45V, 13800 = 13.80V)
            rawMv >= 9000 -> rawMv / 1000.0f

            // Raw car centivolts (e.g. 1245 = 12.45V)
            rawMv in 900..1800 -> rawMv / 100.0f

            // Kernel dummy battery proxy (3400mV - 4200mV) mapped to Automotive Lead-Acid curve
            rawMv in 3200..4300 -> {
                11.2f + ((rawMv - 3200f) / 1000f) * 3.2f
            }

            else -> 12.6f
        }
    }

    // 3. Sysfs Kernel ADC Hardware Reader (Concerto direct /sys bus fallback)
    private val sysfsPollRunnable = object : Runnable {
        override fun run() {
            if (!isPollingSysfs) return
            val sysfsV = readSysfsVoltage()
            if (sysfsV > 8.0f) {
                processVoltageReading(sysfsV)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun readSysfsVoltage(): Float {
        val candidatePaths = arrayOf(
            "/sys/class/power_supply/battery/voltage_now",
            "/sys/class/power_supply/battery/batt_vol",
            "/sys/devices/platform/soc/by-name/battery/voltage",
            "/sys/devices/platform/vehicle/voltage"
        )
        for (path in candidatePaths) {
            try {
                val f = File(path)
                if (f.exists() && f.canRead()) {
                    val content = f.readText().trim()
                    val value = content.toLongOrNull() ?: continue
                    // If in microvolts (e.g. 13800000 uV = 13.8V)
                    if (value > 8_000_000) return value / 1_000_000.0f
                    // If in millivolts (e.g. 13800 mV = 13.8V)
                    if (value > 8_000) return value / 1000.0f
                }
            } catch (e: Exception) {}
        }
        return -1.0f
    }

    private fun processVoltageReading(measured: Float) {
        // Concerto EMA Precision Smoothing (responsive to sudden drops, stable on small noise)
        filteredVoltage = (filteredVoltage * 0.75f) + (measured * 0.25f)
        val finalVoltage = Math.round(filteredVoltage * 100.0f) / 100.0f

        // Cranking check during initial 8 seconds of ignition
        val elapsed = System.currentTimeMillis() - monitorStartTime
        var isDropAlert = false
        val status: String

        if (elapsed < 8000 && isMonitoringCrank) {
            if (finalVoltage < lowestCrankVoltage) {
                lowestCrankVoltage = finalVoltage
            }
            if (lowestCrankVoltage < 9.6f) {
                isDropAlert = true
                status = "Aki Kritis! (< 9.6V)"
            } else if (lowestCrankVoltage < 10.5f) {
                status = "Starter Lemah"
            } else {
                status = "Starter Sehat (Optimal)"
            }
        } else {
            isMonitoringCrank = false
            status = when {
                finalVoltage >= 13.4f -> "Alternator Mengisi (13.4V+)"
                finalVoltage in 12.4f..13.3f -> "Aki Sehat (Standby)"
                finalVoltage in 11.8f..12.3f -> "Aki Rendah (Perlu Cas)"
                else -> "Aki Soak / Drop!"
            }
        }

        onVoltageUpdate(finalVoltage, isDropAlert, status)
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("com.tw.battery.voltage")
            addAction("com.ts.main.battery")
            addAction("com.microntek.battery")
            addAction("com.forfan.action.BATTERY_VOLTAGE")
        }
        try {
            context.registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {}

        isPollingSysfs = true
        handler.post(sysfsPollRunnable)
    }

    fun stop() {
        isPollingSysfs = false
        handler.removeCallbacks(sysfsPollRunnable)
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {}
    }

    fun getLowestCrankVoltage(): Float = lowestCrankVoltage
}
