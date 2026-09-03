package com.innova.launcher2kd.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.innova.launcher2kd.MainActivity

class CarWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT,
            "com.microntek.bootcheck",
            "com.syu.car.acc.on" -> {
                // Ensure MainActivity is in foreground on fast boot / wake
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
