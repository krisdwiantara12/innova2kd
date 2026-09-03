package com.innova.launcher2kd.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Car Hardware Sentinel
 * Membaca kabel fisik mobil via Headunit MCU (Rem Tangan BRAKE, Lampu Senja ILL, Bluetooth HP)
 * Menyediakan peluncur langsung untuk Radio FM Chip, DSP Equalizer, Phone Dialer, dan Voice Assistant.
 */
class CarHardwareSentinel(
    private val context: Context,
    private val onHandbrakeChanged: (isActive: Boolean) -> Unit,
    private val onHeadlightChanged: (isOn: Boolean) -> Unit,
    private val onBluetoothStatusChanged: (isConnected: Boolean, deviceName: String?) -> Unit
) {

    private var isHandbrakeOn = false
    private var isHeadlightOn = false

    private val hardwareReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return

            // 1. Handbrake (Rem Tangan BRAKE wire)
            if (action == "com.ts.main.brake" || action == "com.tw.brake" || 
                action == "com.syu.car.brake" || action == "com.microntek.brake") {
                val brake = intent.getBooleanExtra("brake", false) || 
                             intent.getIntExtra("brake", 0) == 1 ||
                             intent.getBooleanExtra("isBrake", false)
                isHandbrakeOn = brake
                onHandbrakeChanged(isHandbrakeOn)
            }

            // 2. Headlight (Lampu Senja ILL wire)
            if (action == "com.ts.main.ill" || action == "com.tw.ill" || action == "com.microntek.ill") {
                val ill = intent.getBooleanExtra("ill", false) || 
                          intent.getIntExtra("ill", 0) == 1
                isHeadlightOn = ill
                onHeadlightChanged(isHeadlightOn)
            }

            // 3. Bluetooth Connection Status
            if (action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
                val isConn = state == BluetoothAdapter.STATE_CONNECTED
                onBluetoothStatusChanged(isConn, null)
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction("com.ts.main.brake")
            addAction("com.tw.brake")
            addAction("com.syu.car.brake")
            addAction("com.microntek.brake")
            addAction("com.ts.main.ill")
            addAction("com.tw.ill")
            addAction("com.microntek.ill")
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        try {
            ContextCompat.registerReceiver(context, hardwareReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } catch (e: Exception) {}

        // Inisialisasi awal lampu senja dari night mode Android
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        onHeadlightChanged(nightMode == Configuration.UI_MODE_NIGHT_YES)

        // Inisialisasi awal status Bluetooth
        checkInitialBluetoothStatus()
    }

    private fun checkInitialBluetoothStatus() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                val a2dpConnected = adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
                val headsetConnected = adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
                val isConn = a2dpConnected || headsetConnected
                onBluetoothStatusChanged(isConn, if (isConn) "HP Terhubung" else null)
            } else {
                onBluetoothStatusChanged(false, null)
            }
        } catch (e: Exception) {
            onBluetoothStatusChanged(false, null)
        }
    }

    fun stop() {
        try {
            context.unregisterReceiver(hardwareReceiver)
        } catch (e: Exception) {}
    }

    // ==================== APP LAUNCHERS FOR CONCERTO HARDWARE ====================

    fun launchRadioApp(): Boolean {
        val radioPackages = arrayOf(
            "com.ts.radio",
            "com.tw.radio",
            "com.microntek.radio",
            "com.syu.radio",
            "com.android.fmradio",
            "com.forfun.radio"
        )
        return tryLaunchPackages(radioPackages, "Radio FM")
    }

    fun launchBluetoothPhoneApp(): Boolean {
        val btPackages = arrayOf(
            "com.ts.bt",
            "com.tw.bt",
            "com.microntek.bluetooth",
            "com.syu.bt",
            "com.android.dialer"
        )
        if (tryLaunchPackages(btPackages, "Bluetooth Telepon")) return true

        // Fallback ke intent dialer standar
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun launchEqualizerDspApp(): Boolean {
        val eqPackages = arrayOf(
            "com.ts.eq",
            "com.tw.eq",
            "com.microntek.equalizer",
            "com.syu.eq",
            "com.android.soundsettings"
        )
        if (tryLaunchPackages(eqPackages, "DSP Equalizer")) return true

        // Fallback ke panel kontrol efek audio sistem
        return try {
            val eqIntent = Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(eqIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun launchVoiceAssistant(): Boolean {
        return try {
            val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(voiceIntent)
            true
        } catch (e: Exception) {
            try {
                val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(assistIntent)
                true
            } catch (e2: Exception) {
                Toast.makeText(context, "Asisten Suara Google tidak ditemukan", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    fun launchAuxCamera(): Boolean {
        val camPackages = arrayOf(
            "com.ts.aux",
            "com.tw.aux",
            "com.microntek.aux",
            "com.syu.aux",
            "com.android.camerastream"
        )
        val launched = tryLaunchPackages(camPackages, "Kamera Parkir / AUX")
        if (!launched) {
            Toast.makeText(context, "Kamera AUX / AV-IN tidak terdeteksi", Toast.LENGTH_SHORT).show()
        }
        return launched
    }

    fun launchSteeringKeyLearning(): Boolean {
        val swcPackages = arrayOf(
            "com.ts.key",
            "com.tw.steering",
            "com.microntek.wheel",
            "com.syu.car.wheel",
            "com.android.settings"
        )
        val launched = tryLaunchPackages(swcPackages, "Kalibrasi Tombol Setir")
        if (!launched) {
            Toast.makeText(context, "Pengaturan Tombol Setir tidak terdeteksi", Toast.LENGTH_SHORT).show()
        }
        return launched
    }

    fun launchDvrDashcam(): Boolean {
        val dvrPackages = arrayOf(
            "com.ts.dvr",
            "com.tw.dvr",
            "com.microntek.dvr",
            "com.syu.dvr"
        )
        val launched = tryLaunchPackages(dvrPackages, "USB Dashcam DVR")
        if (!launched) {
            Toast.makeText(context, "Aplikasi USB Dashcam tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
        return launched
    }

    fun launchUsbVideoPlayer(): Boolean {
        val videoPackages = arrayOf(
            "com.ts.video",
            "com.tw.video",
            "com.microntek.video",
            "com.syu.video",
            "com.android.gallery3d"
        )
        val launched = tryLaunchPackages(videoPackages, "Video Flashdisk")
        if (!launched) {
            Toast.makeText(context, "Pemutar Video tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
        return launched
    }

    private fun tryLaunchPackages(pkgs: Array<String>, appName: String): Boolean {
        val pm = context.packageManager
        for (pkg in pkgs) {
            try {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {}
        }
        return false
    }
}
