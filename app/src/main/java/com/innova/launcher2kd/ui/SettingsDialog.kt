package com.innova.launcher2kd.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.innova.launcher2kd.R
import com.innova.launcher2kd.service.MaintenanceManager
import com.innova.launcher2kd.service.UpdateManager

class SettingsDialog(
    context: Context,
    private val maintenanceManager: MaintenanceManager,
    private val updateManager: UpdateManager,
    private val currentSpeedLimit: Int,
    private val currentBrandName: String,
    private val onSpeedLimitChanged: (Int) -> Unit,
    private val onThemeChanged: (String) -> Unit,
    private val onBrandNameChanged: (String) -> Unit
) : Dialog(context, R.style.DialogTheme) {

    private var selectedSpeedLimit = currentSpeedLimit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_settings)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etVehicleBrand = findViewById<EditText>(R.id.etVehicleBrand)
        val tvSpeedLimitLabel = findViewById<TextView>(R.id.tvSettingSpeedLimitLabel)
        val sbSpeedLimit = findViewById<SeekBar>(R.id.sbSpeedLimit)
        val etOdo = findViewById<EditText>(R.id.etOdometer)
        val rgTheme = findViewById<RadioGroup>(R.id.rgTheme)
        val tvSettingVersionName = findViewById<TextView>(R.id.tvSettingVersionName)
        val btnCheckUpdateNow = findViewById<Button>(R.id.btnCheckUpdateNow)
        val tvUpdateStatusText = findViewById<TextView>(R.id.tvUpdateStatusText)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnCancel = findViewById<Button>(R.id.btnCancelSettings)

        // Init Brand Name
        etVehicleBrand.setText(currentBrandName)

        // Init Version Name
        tvSettingVersionName.text = "Versi Terpasang: v${updateManager.getLocalVersionName()}"

        // Tombol Cek Pembaruan Manual
        btnCheckUpdateNow.setOnClickListener {
            tvUpdateStatusText.text = "Sedang memeriksa GitHub krisdwiantara12/innova2kd..."
            btnCheckUpdateNow.isEnabled = false
            updateManager.checkForUpdates(
                isManual = true,
                onManualResult = { _, message: String ->
                    btnCheckUpdateNow.post {
                        btnCheckUpdateNow.isEnabled = true
                        tvUpdateStatusText.text = message
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Init Speed Limit: Range 80 to 140
        val progress = (currentSpeedLimit - 80).coerceIn(0, 60)
        sbSpeedLimit.progress = progress
        tvSpeedLimitLabel.text = "Batas Peringatan Kecepatan (Tol): $currentSpeedLimit KM/H"

        sbSpeedLimit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, prog: Int, fromUser: Boolean) {
                selectedSpeedLimit = 80 + prog
                tvSpeedLimitLabel.text = "Batas Peringatan Kecepatan (Tol): $selectedSpeedLimit KM/H"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Init Odometer
        etOdo.setText(maintenanceManager.getTotalOdometerKm().toString())

        btnSave.setOnClickListener {
            // 0. Save Custom Brand Name
            val brandInput = etVehicleBrand.text.toString().trim()
            val finalBrand = if (brandInput.isNotEmpty()) brandInput else "SANEPO"
            onBrandNameChanged(finalBrand)

            // 1. Save Speed Limit
            onSpeedLimitChanged(selectedSpeedLimit)

            // 2. Save Odometer
            val newOdo = etOdo.text.toString().toIntOrNull()
            if (newOdo != null && newOdo > 0) {
                maintenanceManager.setBaseOdometerKm(newOdo)
            }

            // 3. Save Theme
            val themeStr = when (rgTheme.checkedRadioButtonId) {
                R.id.rbCyan -> "CYAN"
                R.id.rbSilver -> "SILVER"
                else -> "AMBER"
            }
            onThemeChanged(themeStr)

            Toast.makeText(context, "Pengaturan Berhasil Disimpan", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }
    }
}
