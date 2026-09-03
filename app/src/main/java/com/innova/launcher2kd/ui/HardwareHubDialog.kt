package com.innova.launcher2kd.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import com.innova.launcher2kd.R
import com.innova.launcher2kd.service.CarHardwareSentinel
import com.innova.launcher2kd.service.MaintenanceManager
import java.util.Locale

class HardwareHubDialog(
    context: Context,
    private val carHardwareSentinel: CarHardwareSentinel,
    private val maintenanceManager: MaintenanceManager,
    private val onOpenDspRequested: () -> Unit
) : Dialog(context, R.style.DialogTheme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_hardware_hub)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        findViewById<Button>(R.id.btnCloseHub).setOnClickListener {
            dismiss()
        }

        // 1. Kamera Parkir / AUX
        findViewById<ViewGroup>(R.id.btnHubCamera).setOnClickListener {
            dismiss()
            carHardwareSentinel.launchAuxCamera()
        }

        // 2. Kalibrasi Tombol Setir
        findViewById<ViewGroup>(R.id.btnHubSwc).setOnClickListener {
            dismiss()
            carHardwareSentinel.launchSteeringKeyLearning()
        }

        // 3. USB Dashcam DVR
        findViewById<ViewGroup>(R.id.btnHubDvr).setOnClickListener {
            dismiss()
            carHardwareSentinel.launchDvrDashcam()
        }

        // 4. Pemutar Video Flashdisk
        findViewById<ViewGroup>(R.id.btnHubVideo).setOnClickListener {
            dismiss()
            carHardwareSentinel.launchUsbVideoPlayer()
        }

        // 5. Kalkulator Biaya Solar 2KD
        findViewById<ViewGroup>(R.id.btnHubFuel).setOnClickListener {
            showFuelCostCalculator()
        }

        // 6. Equalizer DSP
        findViewById<ViewGroup>(R.id.btnHubDsp).setOnClickListener {
            dismiss()
            val launched = carHardwareSentinel.launchEqualizerDspApp()
            if (!launched) {
                onOpenDspRequested()
            }
        }
    }

    private fun showFuelCostCalculator() {
        val dist = maintenanceManager.getSessionDistanceKm()
        val liters = maintenanceManager.getEstimatedSolarLiters()
        val costBio = maintenanceManager.getEstimatedFuelCostRp("BIOSOLAR")
        val costDexlite = maintenanceManager.getEstimatedFuelCostRp("DEXLITE")
        val costDex = maintenanceManager.getEstimatedFuelCostRp("PERTAMINA_DEX")

        val msg = """
            Jarak Trip Sesi: ${String.format(Locale.US, "%.1f", dist)} KM
            Est. Konsumsi: ${String.format(Locale.US, "%.2f", liters)} Liter (~11.5 km/L)

            💰 ESTIMASI BIAYA SOLAR TRIP:
            • Biosolar B35 (Rp 6.800): Rp ${String.format(Locale.US, "%,d", costBio)}
            • Dexlite (Rp 14.550): Rp ${String.format(Locale.US, "%,d", costDexlite)}
            • Pertamina Dex (Rp 15.650): Rp ${String.format(Locale.US, "%,d", costDex)}

            Tangki 2KD: Kapasitas 55 Liter (~630 KM)
        """.trimIndent()

        AlertDialog.Builder(context)
            .setTitle("⛽ KALKULATOR BBM INNOVA 2KD")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }
}
