package com.innova.launcher2kd.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowManager
import java.util.Calendar

class AutoDimmer(
    private val activity: Activity
) {
    private var isEnabled = true
    private var isNightDimmed = false

    private val headlightReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == "android.intent.action.HEADLIGHT_ON" || action == "com.microntek.HEADLIGHT_ON") {
                applyDimming(true)
            } else if (action == "android.intent.action.HEADLIGHT_OFF" || action == "com.microntek.HEADLIGHT_OFF") {
                applyDimming(false)
            }
        }
    }

    fun start() {
        try {
            val filter = IntentFilter().apply {
                addAction("android.intent.action.HEADLIGHT_ON")
                addAction("android.intent.action.HEADLIGHT_OFF")
                addAction("com.microntek.HEADLIGHT_ON")
                addAction("com.microntek.HEADLIGHT_OFF")
            }
            activity.registerReceiver(headlightReceiver, filter)
        } catch (e: Exception) {
            // Receiver registration fallback
        }
        checkTimeBasedDimming()
    }

    fun stop() {
        try {
            activity.unregisterReceiver(headlightReceiver)
        } catch (e: Exception) {}
    }

    fun setAutoDimmingEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            applyDimming(false)
        } else {
            checkTimeBasedDimming()
        }
    }

    fun checkTimeBasedDimming() {
        if (!isEnabled) return
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour >= 18 || currentHour < 6
        applyDimming(isNightTime)
    }

    private fun applyDimming(dim: Boolean) {
        if (!isEnabled && dim) return
        isNightDimmed = dim

        activity.runOnUiThread {
            val lp = activity.window.attributes
            lp.screenBrightness = if (dim) 0.35f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity.window.attributes = lp
        }
    }

    fun isDimmed(): Boolean = isNightDimmed
}
