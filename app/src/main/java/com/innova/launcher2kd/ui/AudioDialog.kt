package com.innova.launcher2kd.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.innova.launcher2kd.R
import com.innova.launcher2kd.audio.AudioDspSuite

class AudioDialog(
    context: Context,
    private val audioDsp: AudioDspSuite
) : Dialog(context, R.style.DialogTheme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_audio_dsp)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        findViewById<Button>(R.id.btnCloseAudio).setOnClickListener {
            dismiss()
        }

        // Preset Buttons
        findViewById<Button>(R.id.btnPresetClarity).setOnClickListener {
            audioDsp.applyPresetDieselClarity()
            Toast.makeText(context, "Preset: Innova Diesel Clarity Aktif", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnPresetBass).setOnClickListener {
            audioDsp.applyPresetDeepBass()
            Toast.makeText(context, "Preset: Deep Bass Punch Aktif", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnPresetVocal).setOnClickListener {
            audioDsp.applyPresetVocal()
            Toast.makeText(context, "Preset: Vocal Aktif", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnPresetFlat).setOnClickListener {
            audioDsp.applyPresetFlat()
            Toast.makeText(context, "Preset: Flat Aktif", Toast.LENGTH_SHORT).show()
        }

        // Cabin Sound Stage Buttons
        findViewById<Button>(R.id.btnStageDriver).setOnClickListener {
            Toast.makeText(context, "Sound Stage: Fokus Sopir (Kanan Depan)", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStageAll).setOnClickListener {
            Toast.makeText(context, "Sound Stage: Merata 3 Baris", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStageFront).setOnClickListener {
            Toast.makeText(context, "Sound Stage: Hanya Baris Depan", Toast.LENGTH_SHORT).show()
        }

        // SVC Switch
        val swSvc = findViewById<SwitchCompat>(R.id.swSvc)
        swSvc.isChecked = audioDsp.isSvcEnabled
        swSvc.setOnCheckedChangeListener { _, isChecked ->
            audioDsp.isSvcEnabled = isChecked
            val msg = if (isChecked) "SVC Aktif (Volume menyesuaikan kecepatan)" else "SVC Nonaktif"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
