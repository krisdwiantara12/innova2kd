package com.innova.launcher2kd.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.innova.launcher2kd.R
import com.innova.launcher2kd.service.Obd2Manager

class Obd2Dialog(
    context: Context,
    private val obd2Manager: Obd2Manager,
    private val onDeviceSelected: (address: String) -> Unit
) : Dialog(context, R.style.DialogTheme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_obd2)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        findViewById<Button>(R.id.btnCloseObd).setOnClickListener { dismiss() }

        val container = findViewById<LinearLayout>(R.id.containerObdDevices)
        val tvStatus = findViewById<TextView>(R.id.tvObdConnectionStatus)
        val btnDisconnect = findViewById<Button>(R.id.btnDisconnectObd)

        btnDisconnect.setOnClickListener {
            obd2Manager.stop()
            tvStatus.text = "Status: Terputus"
            Toast.makeText(context, "Koneksi OBD2 telah diputus", Toast.LENGTH_SHORT).show()
        }

        populatePairedDevices(container, tvStatus)
    }

    @SuppressLint("MissingPermission")
    private fun populatePairedDevices(container: LinearLayout, tvStatus: TextView) {
        val pairedDevices = obd2Manager.getPairedDevices()
        container.removeAllViews()

        if (pairedDevices.isEmpty()) {
            val emptyTv = TextView(context).apply {
                text = "Tidak ada perangkat Bluetooth yang ter-pair.\nBuka Pengaturan Android Headunit -> Bluetooth -> Cari & Pasangkan (Pair) ke 'OBDII' atau 'ELM327' (PIN biasanya 1234 atau 0000)."
                setTextColor(context.getColor(R.color.text_dim))
                textSize = 11f
                setPadding(10, 10, 10, 10)
            }
            container.addView(emptyTv)
            return
        }

        for (dev in pairedDevices) {
            val name = dev.name ?: "Perangkat Tanpa Nama"
            val addr = dev.address

            val btn = Button(context).apply {
                text = "⚡ $name ($addr)"
                setTextColor(context.getColor(R.color.text_primary))
                textSize = 11f
                setBackgroundResource(R.drawable.bg_button_shortcut)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8)
                }

                setOnClickListener {
                    tvStatus.text = "Menghubungkan ke $name..."
                    onDeviceSelected(addr)
                    Toast.makeText(context, "Mencoba koneksi ke $name...", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
            container.addView(btn)
        }
    }
}
