package com.innova.launcher2kd.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.innova.launcher2kd.R

class UpdateDialog(
    context: Context,
    private val versionName: String,
    private val changelog: String,
    private val onUpdateClicked: () -> Unit
) : Dialog(context, R.style.DialogTheme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_update)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.75).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        findViewById<TextView>(R.id.tvUpdateTitle).text = "Pembaruan Tersedia (v$versionName)"
        findViewById<TextView>(R.id.tvUpdateChangelog).text = changelog

        findViewById<Button>(R.id.btnUpdateNow).setOnClickListener {
            onUpdateClicked()
            dismiss()
        }

        findViewById<Button>(R.id.btnUpdateLater).setOnClickListener {
            dismiss()
        }
    }
}
