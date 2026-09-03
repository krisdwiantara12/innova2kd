package com.innova.launcher2kd.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.innova.launcher2kd.R
import com.innova.launcher2kd.model.AppItem

class AppPickerDialog(
    context: Context,
    private val slotTitle: String,
    private val onAppChosen: (AppItem) -> Unit
) : Dialog(context, R.style.DialogTheme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_app_picker)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        findViewById<TextView>(R.id.tvPickerTitle).text = "PILIH APLIKASI: $slotTitle"

        val rv = findViewById<RecyclerView>(R.id.rvPickerApps)
        rv.layoutManager = LinearLayoutManager(context)

        val installedApps = loadInstalledApps()
        val adapter = AppPickerAdapter(installedApps) { selectedApp ->
            onAppChosen(selectedApp)
            dismiss()
        }
        rv.adapter = adapter

        val etSearch = findViewById<EditText>(R.id.etPickerSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<Button>(R.id.btnPickerClose).setOnClickListener {
            dismiss()
        }
    }

    private fun loadInstalledApps(): List<AppItem> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<AppItem>()

        for (info in resolveInfoList) {
            val pkg = info.activityInfo.packageName
            // Hide this launcher from being selected as its own shortcut
            if (pkg == context.packageName) continue

            val label = info.loadLabel(pm).toString()
            val icon = info.loadIcon(pm)
            list.add(AppItem(label, pkg, icon))
        }
        return list.sortedBy { it.label.lowercase() }
    }
}
