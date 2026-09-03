package com.innova.launcher2kd

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.innova.launcher2kd.audio.AudioDspSuite
import com.innova.launcher2kd.service.AutoDimmer
import com.innova.launcher2kd.service.BatterySentinel
import com.innova.launcher2kd.service.GpsSpeedManager
import com.innova.launcher2kd.service.MaintenanceManager
import com.innova.launcher2kd.service.Obd2Manager
import com.innova.launcher2kd.service.UpdateManager
import com.innova.launcher2kd.ui.AppDrawerDialog
import com.innova.launcher2kd.ui.AppPickerDialog
import com.innova.launcher2kd.ui.AudioDialog
import com.innova.launcher2kd.ui.FuseBoxDialog
import com.innova.launcher2kd.ui.Obd2Dialog
import com.innova.launcher2kd.ui.SettingsDialog
import com.innova.launcher2kd.ui.UpdateDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Services & Managers
    private lateinit var gpsSpeedManager: GpsSpeedManager
    private lateinit var batterySentinel: BatterySentinel
    private lateinit var audioDspSuite: AudioDspSuite
    private lateinit var maintenanceManager: MaintenanceManager
    private lateinit var updateManager: UpdateManager
    private lateinit var obd2Manager: Obd2Manager
    private lateinit var autoDimmer: AutoDimmer
    private lateinit var audioManager: AudioManager
    private lateinit var prefs: SharedPreferences

    // UI Views - Top Bar
    private lateinit var tvGpsStatus: TextView
    private lateinit var tvBatteryVoltage: TextView
    private lateinit var tvCockpitBrand: TextView
    private lateinit var tvTotalOdo: TextView
    private lateinit var tvClock: TextView
    private lateinit var btnScreenOff: View

    // UI Views - Left (3D Speedometer Cluster & OBD2 Telemetry)
    private lateinit var tvSpeedNumber: TextView
    private lateinit var tvSpeedUnit: TextView
    private lateinit var tvSpeedLimit: TextView
    private lateinit var llObdTelemetry: View
    private lateinit var tvTurboBoost: TextView
    private lateinit var tvCoolantTemp: TextView
    private lateinit var tvHeading: TextView
    private lateinit var tvAltitude: TextView

    // UI Views - Center (Trip Computer & Diagnostics)
    private lateinit var tvObdStatus: TextView
    private lateinit var tvTripDistance: TextView
    private lateinit var tvTripTime: TextView
    private lateinit var tvTripAvgSpeed: TextView
    private lateinit var btnResetTrip: Button
    private lateinit var tvCrankingVoltage: TextView
    private lateinit var tvServiceOilKm: TextView
    private lateinit var pbServiceOil: ProgressBar
    private lateinit var tvServiceFuelKm: TextView
    private lateinit var pbServiceFuel: ProgressBar
    private lateinit var tvBioSolarAlert: TextView
    private lateinit var tvEngineHours: TextView

    // UI Views - Right (3D App Shortcuts)
    private lateinit var btnShortcut1: View
    private lateinit var tvName1: TextView
    private lateinit var ivIcon1: ImageView
    private lateinit var btnEditSc1: View

    private lateinit var btnShortcut2: View
    private lateinit var tvName2: TextView
    private lateinit var ivIcon2: ImageView
    private lateinit var btnEditSc2: View

    private lateinit var btnShortcut3: View
    private lateinit var tvName3: TextView
    private lateinit var ivIcon3: ImageView
    private lateinit var btnEditSc3: View

    private lateinit var btnAllApps: View

    // UI Views - Bottom Bar
    private lateinit var sbVolume: SeekBar
    private lateinit var btnMute: Button
    private lateinit var btnOpenAudio: View
    private lateinit var btnOpenFuse: View
    private lateinit var btnOpenSettings: View
    private lateinit var screenOffOverlay: View

    // State Variables
    private var speedLimit = 100
    private var currentSpeed = 0
    private var cockpitBrand = "SANEPO"
    private var pkgShortcut1 = "com.google.android.apps.maps"
    private var pkgShortcut2 = "com.spotify.music"
    private var pkgShortcut3 = "com.android.fmradio"
    private var activeTheme = "AMBER"
    private var isAutoDimmingEnabled = true

    // Persistent Trip Computer State (Rest area retention 2 hours)
    private var tripDistanceKm = 0.0f
    private var tripDriveTimeSec = 0L
    private var tripStartTime = System.currentTimeMillis()
    private var isMuted = false
    private var preMuteVolume = 8

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Permission Launcher
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            gpsSpeedManager.startListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = getSharedPreferences("innova_launcher_config", Context.MODE_PRIVATE)
        loadSavedConfig()

        initServices()
        bindViews()
        setupListeners()
        startClockUpdates()
        applyCockpitTheme(activeTheme)
    }

    private fun loadSavedConfig() {
        speedLimit = prefs.getInt("speed_limit", 100)
        cockpitBrand = prefs.getString("cockpit_brand", "SANEPO") ?: "SANEPO"
        activeTheme = prefs.getString("cockpit_theme", "AMBER") ?: "AMBER"
        isAutoDimmingEnabled = prefs.getBoolean("auto_dimming_enabled", true)
        pkgShortcut1 = prefs.getString("pkg_sc1", "com.google.android.apps.maps") ?: "com.google.android.apps.maps"
        pkgShortcut2 = prefs.getString("pkg_sc2", "com.spotify.music") ?: "com.spotify.music"
        pkgShortcut3 = prefs.getString("pkg_sc3", "com.android.fmradio") ?: "com.android.fmradio"
    }

    private fun initServices() {
        maintenanceManager = MaintenanceManager(this)
        audioDspSuite = AudioDspSuite(this)
        autoDimmer = AutoDimmer(this)
        autoDimmer.setAutoDimmingEnabled(isAutoDimmingEnabled)

        // GPS Speed Manager (Precision with Altitude, Cardinal Heading, and Haversine Distance)
        gpsSpeedManager = GpsSpeedManager(
            this,
            onSpeedUpdate = { speed, altitude, heading, _ ->
                runOnUiThread {
                    currentSpeed = speed
                    tvSpeedNumber.text = speed.toString()
                    tvAltitude.text = "$altitude MDPL"
                    tvHeading.text = heading

                    // Speed limit warning
                    if (speed > speedLimit) {
                        tvSpeedNumber.setTextColor(ContextCompat.getColor(this, R.color.status_danger))
                        tvSpeedLimit.setTextColor(ContextCompat.getColor(this, R.color.status_danger))
                    } else {
                        tvSpeedNumber.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                        tvSpeedLimit.setTextColor(getThemeAccentColor())
                    }

                    // Speed-Compensated Volume (SVC)
                    audioDspSuite.onSpeedChanged(speed)
                }
            },
            onDistanceUpdate = { deltaKm ->
                runOnUiThread {
                    tripDistanceKm += deltaKm.toFloat()
                    maintenanceManager.addDistance(deltaKm)
                    saveTripState()
                    updateMaintenanceViews()
                    updateTripComputerViews()
                }
            },
            onGpsStatusChanged = { isLocked ->
                runOnUiThread {
                    if (isLocked) {
                        tvGpsStatus.text = "GPS: LOCK (SATELIT 3D)"
                        tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green))
                    } else {
                        tvGpsStatus.text = "GPS: MENCARI SATELIT..."
                        tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                    }
                }
            }
        )

        // Battery Sentinel (Real voltage & Cranking drop detection)
        batterySentinel = BatterySentinel(this) { voltage, isCrankingDrop, status ->
            runOnUiThread {
                tvBatteryVoltage.text = String.format(Locale.US, "AKI: %.1fV", voltage)
                tvCrankingVoltage.text = String.format(Locale.US, "%.1fV (%s)", voltage, status)
                if (isCrankingDrop) {
                    tvCrankingVoltage.setTextColor(ContextCompat.getColor(this, R.color.status_danger))
                    Toast.makeText(this, "PERINGATAN 2KD: Drop voltase starter! Aki perlu dicek.", Toast.LENGTH_LONG).show()
                } else {
                    tvCrankingVoltage.setTextColor(ContextCompat.getColor(this, R.color.status_green))
                }
            }
        }

        // OBD2 Bluetooth Manager (ELM327 for 2KD-FTV)
        obd2Manager = Obd2Manager(
            this,
            onDataReceived = { voltage, ect, boostBar, rpm ->
                runOnUiThread {
                    // Update Turbo Boost
                    tvTurboBoost.text = String.format(Locale.US, "TURBO: +%.1f BAR", boostBar)

                    // Update Coolant Temp (ECT)
                    if (ect != -999) {
                        tvCoolantTemp.text = "SUHU: $ect°C"
                        if (ect >= 100) {
                            tvCoolantTemp.setTextColor(ContextCompat.getColor(this, R.color.status_danger))
                            Toast.makeText(this, "⚠️ OVERHEAT WARNING: Suhu mesin $ect°C!", Toast.LENGTH_SHORT).show()
                        } else {
                            tvCoolantTemp.setTextColor(ContextCompat.getColor(this, R.color.status_green))
                        }
                    }

                    // Update Real Battery Voltage from OBD2 Pin 16 if valid
                    if (voltage > 9.0f) {
                        tvBatteryVoltage.text = String.format(Locale.US, "AKI: %.1fV", voltage)
                    }
                }
            },
            onStatusChanged = { _, isConnected ->
                runOnUiThread {
                    if (isConnected) {
                        // Smooth reveal animation
                        if (llObdTelemetry.visibility != View.VISIBLE) {
                            llObdTelemetry.alpha = 0f
                            llObdTelemetry.visibility = View.VISIBLE
                            llObdTelemetry.animate()
                                .alpha(1f)
                                .setDuration(400)
                                .start()

                            tvObdStatus.alpha = 0f
                            tvObdStatus.visibility = View.VISIBLE
                            tvObdStatus.text = "⚡ 2KD ECU ONLINE"
                            tvObdStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green))
                            tvObdStatus.animate()
                                .alpha(1f)
                                .setDuration(400)
                                .start()

                            Toast.makeText(this, "⚡ ELM327 Terhubung: Telemetri 2KD Aktif!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Smooth hide animation (tetap bersih jika ELM327 tidak terpasang)
                        if (llObdTelemetry.visibility == View.VISIBLE) {
                            llObdTelemetry.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction { llObdTelemetry.visibility = View.GONE }
                                .start()

                            tvObdStatus.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction { tvObdStatus.visibility = View.GONE }
                                .start()
                        } else {
                            llObdTelemetry.visibility = View.GONE
                            tvObdStatus.visibility = View.GONE
                        }
                    }
                }
            }
        )

        // OTA Auto-Update Manager (Official GitHub Integration)
        updateManager = UpdateManager(
            this,
            onUpdateAvailable = { versionName, changelog, apkUrl ->
                runOnUiThread {
                    UpdateDialog(this, versionName, changelog) {
                        Toast.makeText(this, "Mengunduh pembaruan dari GitHub...", Toast.LENGTH_SHORT).show()
                        updateManager.downloadAndInstall(apkUrl, versionName)
                    }.show()
                }
            }
        )
        updateManager.checkForUpdates()

        // Init Persistent Trip Computer
        initTripComputer()
    }

    private fun initTripComputer() {
        val lastTimestamp = prefs.getLong("trip_last_timestamp", 0L)
        val now = System.currentTimeMillis()
        val diffHours = (now - lastTimestamp) / (1000 * 3600.0)

        // Jika jeda mesin mati kurang dari 2 jam (rest area / SPBU), lanjutkan trip!
        if (diffHours < 2.0 && lastTimestamp > 0) {
            tripDistanceKm = prefs.getFloat("trip_dist_km", 0.0f)
            tripDriveTimeSec = prefs.getLong("trip_drive_time_sec", 0L)
            tripStartTime = now - (tripDriveTimeSec * 1000)
        } else {
            // Mulai trip baru jika mobil mati lebih dari 2 jam
            tripDistanceKm = 0.0f
            tripDriveTimeSec = 0L
            tripStartTime = now
            saveTripState()
        }
    }

    private fun saveTripState() {
        val elapsedSec = (System.currentTimeMillis() - tripStartTime) / 1000
        prefs.edit()
            .putFloat("trip_dist_km", tripDistanceKm)
            .putLong("trip_drive_time_sec", elapsedSec)
            .putLong("trip_last_timestamp", System.currentTimeMillis())
            .apply()
    }

    private fun resetTripManual() {
        tripDistanceKm = 0.0f
        tripDriveTimeSec = 0L
        tripStartTime = System.currentTimeMillis()
        saveTripState()
        updateTripComputerViews()
        Toast.makeText(this, "Trip Computer berhasil di-reset ke 0 KM", Toast.LENGTH_SHORT).show()
    }

    private fun bindViews() {
        // Top status & branding
        tvGpsStatus = findViewById(R.id.tvGpsStatus)
        tvBatteryVoltage = findViewById(R.id.tvBatteryVoltage)
        tvCockpitBrand = findViewById(R.id.tvCockpitBrand)
        tvTotalOdo = findViewById(R.id.tvTotalOdo)
        tvClock = findViewById(R.id.tvClock)
        btnScreenOff = findViewById(R.id.btnScreenOff)

        // Left Speedometer & OBD2 Telemetry
        tvSpeedNumber = findViewById(R.id.tvSpeedNumber)
        tvSpeedUnit = findViewById(R.id.tvSpeedUnit)
        tvSpeedLimit = findViewById(R.id.tvSpeedLimit)
        llObdTelemetry = findViewById(R.id.llObdTelemetry)
        tvTurboBoost = findViewById(R.id.tvTurboBoost)
        tvCoolantTemp = findViewById(R.id.tvCoolantTemp)
        tvHeading = findViewById(R.id.tvHeading)
        tvAltitude = findViewById(R.id.tvAltitude)

        // Sembunyikan modul OBD2 hingga ELM327 benar-benar tersambung
        llObdTelemetry.visibility = View.GONE

        // Center Telemetry & Trip
        tvObdStatus = findViewById(R.id.tvObdStatus)
        tvTripDistance = findViewById(R.id.tvTripDistance)
        tvTripTime = findViewById(R.id.tvTripTime)
        tvTripAvgSpeed = findViewById(R.id.tvTripAvgSpeed)
        btnResetTrip = findViewById(R.id.btnResetTrip)
        tvCrankingVoltage = findViewById(R.id.tvCrankingVoltage)
        tvServiceOilKm = findViewById(R.id.tvServiceOilKm)
        pbServiceOil = findViewById(R.id.pbServiceOil)
        tvServiceFuelKm = findViewById(R.id.tvServiceFuelKm)
        pbServiceFuel = findViewById(R.id.pbServiceFuel)
        tvBioSolarAlert = findViewById(R.id.tvBioSolarAlert)
        tvEngineHours = findViewById(R.id.tvEngineHours)

        // Right Shortcuts
        btnShortcut1 = findViewById(R.id.btnShortcut1)
        tvName1 = findViewById(R.id.tvName1)
        ivIcon1 = findViewById(R.id.ivIcon1)
        btnEditSc1 = findViewById(R.id.btnEditSc1)

        btnShortcut2 = findViewById(R.id.btnShortcut2)
        tvName2 = findViewById(R.id.tvName2)
        ivIcon2 = findViewById(R.id.ivIcon2)
        btnEditSc2 = findViewById(R.id.btnEditSc2)

        btnShortcut3 = findViewById(R.id.btnShortcut3)
        tvName3 = findViewById(R.id.tvName3)
        ivIcon3 = findViewById(R.id.ivIcon3)
        btnEditSc3 = findViewById(R.id.btnEditSc3)

        btnAllApps = findViewById(R.id.btnAllApps)

        // Bottom Bar
        sbVolume = findViewById(R.id.sbVolume)
        btnMute = findViewById(R.id.btnMute)
        btnOpenAudio = findViewById(R.id.btnOpenAudio)
        btnOpenFuse = findViewById(R.id.btnOpenFuse)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)
        screenOffOverlay = findViewById(R.id.screenOffOverlay)

        // Apply initial text & brand
        tvCockpitBrand.text = "✦ $cockpitBrand ✦"
        tvSpeedLimit.text = "BATAS: $speedLimit KM/H"

        initVolumeSlider()
        updateMaintenanceViews()
        updateTripComputerViews()
        updateShortcutLabels()
    }

    private fun initVolumeSlider() {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        sbVolume.max = maxVol
        sbVolume.progress = curVol
    }

    private fun updateMaintenanceViews() {
        val totalOdo = maintenanceManager.getTotalOdometerKm()
        tvTotalOdo.text = String.format(Locale.US, "ODO: %,d KM", totalOdo)

        val oilRem = maintenanceManager.getOilRemainingKm()
        tvServiceOilKm.text = "Sisa $oilRem KM"
        pbServiceOil.progress = oilRem

        val fuelFilterRem = maintenanceManager.getFuelFilterRemainingKm()
        tvServiceFuelKm.text = "Sisa $fuelFilterRem KM"
        pbServiceFuel.progress = fuelFilterRem

        // Critical BioSolar B35 Warning (<500 KM)
        if (fuelFilterRem < 500) {
            tvBioSolarAlert.visibility = View.VISIBLE
            tvBioSolarAlert.text = "⚠️ KRITIS BIOSOLAR: Sisa $fuelFilterRem KM! Segera ganti filter solar bawah tangki."
            pbServiceFuel.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_danger))
            tvServiceFuelKm.setTextColor(ContextCompat.getColor(this, R.color.status_danger))
        } else {
            tvBioSolarAlert.visibility = View.GONE
            pbServiceFuel.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_cyan))
            tvServiceFuelKm.setTextColor(ContextCompat.getColor(this, R.color.accent_cyan))
        }

        tvEngineHours.text = "ACC ON: " + maintenanceManager.getEngineHoursFormatted()
    }

    private fun updateTripComputerViews() {
        tvTripDistance.text = String.format(Locale.US, "%.1f KM", tripDistanceKm)

        val elapsedSec = (System.currentTimeMillis() - tripStartTime) / 1000
        val hh = elapsedSec / 3600
        val mm = (elapsedSec % 3600) / 60
        val ss = elapsedSec % 60
        tvTripTime.text = if (hh > 0) String.format("%02d:%02d:%02d", hh, mm, ss) else String.format("%02d:%02d", mm, ss)

        val hours = elapsedSec / 3600.0f
        val avgSpeed = if (hours > 0.005f) (tripDistanceKm / hours) else 0.0f
        tvTripAvgSpeed.text = String.format(Locale.US, "%.0f KM/H", avgSpeed)
    }

    private fun updateShortcutLabels() {
        val pm = packageManager
        bindSingleShortcut(pm, pkgShortcut1, tvName1, ivIcon1, "GOOGLE MAPS")
        bindSingleShortcut(pm, pkgShortcut2, tvName2, ivIcon2, "SPOTIFY / MUSIK")
        bindSingleShortcut(pm, pkgShortcut3, tvName3, ivIcon3, "RADIO FM")
    }

    private fun bindSingleShortcut(
        pm: android.content.pm.PackageManager,
        pkgName: String,
        tvLabel: TextView,
        ivIcon: ImageView,
        fallbackName: String
    ) {
        try {
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            tvLabel.text = pm.getApplicationLabel(appInfo).toString().uppercase()
            ivIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
        } catch (e: Exception) {
            tvLabel.text = fallbackName
        }
    }

    private fun setupListeners() {
        // Screen Off (Stealth Mode)
        btnScreenOff.setOnClickListener {
            screenOffOverlay.visibility = View.VISIBLE
        }
        screenOffOverlay.setOnClickListener {
            screenOffOverlay.visibility = View.GONE
        }

        // Direct App Launching
        btnShortcut1.setOnClickListener { launchPackage(pkgShortcut1) }
        btnShortcut2.setOnClickListener { launchPackage(pkgShortcut2) }
        btnShortcut3.setOnClickListener { launchPackage(pkgShortcut3) }

        // Dedicated Shortcut Picker Buttons (Custom App Shortcuts)
        btnEditSc1.setOnClickListener { openAppPickerForSlot(1) }
        btnEditSc2.setOnClickListener { openAppPickerForSlot(2) }
        btnEditSc3.setOnClickListener { openAppPickerForSlot(3) }

        // Long press also triggers picker
        btnShortcut1.setOnLongClickListener { openAppPickerForSlot(1); true }
        btnShortcut2.setOnLongClickListener { openAppPickerForSlot(2); true }
        btnShortcut3.setOnLongClickListener { openAppPickerForSlot(3); true }

        // Reset Trip Button
        btnResetTrip.setOnClickListener { resetTripManual() }

        // OBD2 Status Pill Click -> Open OBD2 Dialog
        tvObdStatus.setOnClickListener { openObd2Dialog() }

        // All Apps Button
        btnAllApps.setOnClickListener {
            AppDrawerDialog(this).show()
        }

        // Audio DSP Dialog
        btnOpenAudio.setOnClickListener {
            AudioDialog(this, audioDspSuite).show()
        }

        // Fuse Box Guide Dialog
        btnOpenFuse.setOnClickListener {
            FuseBoxDialog(this).show()
        }

        // Settings Dialog (Includes SANEPO branding editor, Auto-Dimming & OBD2)
        btnOpenSettings.setOnClickListener {
            SettingsDialog(
                this,
                maintenanceManager,
                updateManager,
                speedLimit,
                cockpitBrand,
                isAutoDimmingEnabled,
                onSpeedLimitChanged = { newLimit ->
                    speedLimit = newLimit
                    prefs.edit().putInt("speed_limit", speedLimit).apply()
                    tvSpeedLimit.text = "BATAS: $speedLimit KM/H"
                },
                onThemeChanged = { theme ->
                    activeTheme = theme
                    prefs.edit().putString("cockpit_theme", theme).apply()
                    applyCockpitTheme(theme)
                    Toast.makeText(this, "Tema $theme diterapkan", Toast.LENGTH_SHORT).show()
                },
                onBrandNameChanged = { newBrand ->
                    cockpitBrand = newBrand
                    prefs.edit().putString("cockpit_brand", cockpitBrand).apply()
                    tvCockpitBrand.text = "✦ $cockpitBrand ✦"
                    Toast.makeText(this, "Branding kokpit: $cockpitBrand", Toast.LENGTH_SHORT).show()
                },
                onAutoDimmingChanged = { enabled ->
                    isAutoDimmingEnabled = enabled
                    prefs.edit().putBoolean("auto_dimming_enabled", enabled).apply()
                    autoDimmer.setAutoDimmingEnabled(enabled)
                },
                onOpenObd2Requested = {
                    openObd2Dialog()
                }
            ).show()
        }

        // Master Volume Slider
        sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, prog: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, prog, 0)
                    if (prog > 0 && isMuted) {
                        isMuted = false
                        btnMute.text = "🔊 VOL"
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Master Mute Button
        btnMute.setOnClickListener {
            if (!isMuted) {
                preMuteVolume = sbVolume.progress
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                sbVolume.progress = 0
                btnMute.text = "🔇 MUTE"
                isMuted = true
            } else {
                val restore = if (preMuteVolume > 0) preMuteVolume else 5
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restore, 0)
                sbVolume.progress = restore
                btnMute.text = "🔊 VOL"
                isMuted = false
            }
        }
    }

    private fun openObd2Dialog() {
        Obd2Dialog(this, obd2Manager) { selectedAddress ->
            prefs.edit().putString("obd2_last_device", selectedAddress).apply()
            obd2Manager.connectToDevice(selectedAddress)
        }.show()
    }

    private fun openAppPickerForSlot(slot: Int) {
        val slotTitle = when (slot) {
            1 -> "PINTASAN 1 (PETA / NAVIGASI)"
            2 -> "PINTASAN 2 (MUSIK / MEDIA)"
            else -> "PINTASAN 3 (RADIO / LAINNYA)"
        }
        AppPickerDialog(this, slotTitle) { selectedApp ->
            when (slot) {
                1 -> {
                    pkgShortcut1 = selectedApp.packageName
                    prefs.edit().putString("pkg_sc1", pkgShortcut1).apply()
                }
                2 -> {
                    pkgShortcut2 = selectedApp.packageName
                    prefs.edit().putString("pkg_sc2", pkgShortcut2).apply()
                }
                3 -> {
                    pkgShortcut3 = selectedApp.packageName
                    prefs.edit().putString("pkg_sc3", pkgShortcut3).apply()
                }
            }
            updateShortcutLabels()
            Toast.makeText(this, "${selectedApp.label} dipasang ke $slotTitle", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun launchPackage(pkgName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkgName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Aplikasi belum terpasang. Klik tombol [ ⚙ ] untuk mengganti.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka aplikasi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyCockpitTheme(theme: String) {
        val accentColor = when (theme) {
            "CYAN" -> ContextCompat.getColor(this, R.color.accent_cyan)
            "SILVER" -> ContextCompat.getColor(this, R.color.accent_silver)
            else -> ContextCompat.getColor(this, R.color.accent_amber)
        }
        tvCockpitBrand.setTextColor(accentColor)
        tvSpeedUnit.setTextColor(accentColor)
        tvSpeedLimit.setTextColor(accentColor)
        tvAltitude.setTextColor(accentColor)
        tvClock.setTextColor(accentColor)
        tvTripAvgSpeed.setTextColor(accentColor)
        btnMute.setTextColor(accentColor)
    }

    private fun getThemeAccentColor(): Int {
        return when (activeTheme) {
            "CYAN" -> ContextCompat.getColor(this, R.color.accent_cyan)
            "SILVER" -> ContextCompat.getColor(this, R.color.accent_silver)
            else -> ContextCompat.getColor(this, R.color.accent_amber)
        }
    }

    private fun startClockUpdates() {
        clockHandler.post(object : Runnable {
            override fun run() {
                tvClock.text = clockFormat.format(Date())
                updateTripComputerViews()
                autoDimmer.checkTimeBasedDimming()
                clockHandler.postDelayed(this, 1000)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        requestLocationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        batterySentinel.start()
        autoDimmer.start()
        initVolumeSlider()
        updateMaintenanceViews()

        // Auto-reconnect OBD2 if previously paired
        val lastObd = prefs.getString("obd2_last_device", null)
        if (lastObd != null) {
            obd2Manager.connectToDevice(lastObd)
        }
    }

    override fun onPause() {
        super.onPause()
        batterySentinel.stop()
        autoDimmer.stop()
        obd2Manager.stop()
        gpsSpeedManager.stopListening()
        saveTripState()
    }
}
