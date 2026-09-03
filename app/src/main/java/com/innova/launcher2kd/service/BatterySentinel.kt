package com.innova.launcher2kd.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatterySentinel(
    private val context: Context,
    private val onVoltageUpdate: (voltage: Float, isCrankingDrop: Boolean, statusText: String) -> Unit
) {
    private var lowestCrankVoltage = 14.0f
    private var isMonitoringCrank = true
    private val monitorStartTime = System.currentTimeMillis()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val rawMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 12000)
                
                // Headunits usually report actual car voltage in mV (e.g. 13800 = 13.8V) 
                // or normalized tablet mV (e.g. 4100 = 4.1V -> mapped to 12.0 - 14.4V)
                val realCarVoltage = if (rawMv > 8000) {
                    rawMv / 1000.0f
                } else {
                    // Map 3.5V-4.2V tablet battery proxy to 11.5V-14.2V automotive scale
                    11.5f + ((rawMv - 3500f) / 700f) * 2.7f
                }

                // Cranking check during initial 8 seconds of ignition
                val elapsed = System.currentTimeMillis() - monitorStartTime
                var isDropAlert = false
                val status: String

                if (elapsed < 8000 && isMonitoringCrank) {
                    if (realCarVoltage < lowestCrankVoltage) {
                        lowestCrankVoltage = realCarVoltage
                    }
                    if (lowestCrankVoltage < 9.6f) {
                        isDropAlert = true
                        status = "Aki Soak! (< 9.6V)"
                    } else if (lowestCrankVoltage < 10.5f) {
                        status = "Starter Lemah"
                    } else {
                        status = "Starter Sehat"
                    }
                } else {
                    isMonitoringCrank = false
                    status = if (realCarVoltage >= 13.5f) "Alternator Mengisi" else "Mesin Mati / Standby"
                }

                onVoltageUpdate(realCarVoltage, isDropAlert, status)
            }
        }
    }

    fun start() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun stop() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    fun getLowestCrankVoltage(): Float = lowestCrankVoltage
}
