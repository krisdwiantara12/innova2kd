package com.innova.launcher2kd.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.innova.launcher2kd.MainActivity

/**
 * Car Wake & Deep Sleep Receiver
 * Menangani Fast Boot, Wake from Sleep (STR), dan ACC ON/OFF
 * Memastikan Launcher Innova 2KD selalu langsung muncul dan responsif seketika mobil distarter.
 */
class CarWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "com.microntek.bootcheck",
            "com.microntek.acc_on",
            "com.ts.main.acc_on",
            "com.tw.acc_on",
            "com.syu.car.acc.on" -> {
                // Pastikan MainActivity langsung berada di foreground dengan responsif
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    putExtra("FROM_WAKE", true)
                }
                try {
                    context.startActivity(launchIntent)
                } catch (e: Exception) {}
            }
        }
    }
}
