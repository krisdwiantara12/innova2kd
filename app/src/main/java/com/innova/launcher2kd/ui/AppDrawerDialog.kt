package com.innova.launcher2kd.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import com.innova.launcher2kd.R
import com.innova.launcher2kd.model.AppItem

class AppDrawerDialog(
    context: Context,
    private val onAppSelected: ((AppItem) -> Unit)? = null
) : Dialog(context, R.style.DialogTheme) {

    private val appList = mutableListOf<AppItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_app_drawer)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        findViewById<Button>(R.id.btnCloseDrawer).setOnClickListener {
            dismiss()
        }

        loadInstalledApps()

        val gridView = findViewById<GridView>(R.id.gvApps)
        gridView.adapter = AppAdapter(context, appList) { app ->
            if (onAppSelected != null) {
                onAppSelected.invoke(app)
                dismiss()
            } else {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    dismiss()
                }
            }
        }
    }

    private fun loadInstalledApps() {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        appList.clear()
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            // Hide own launcher from the drawer to prevent recursion
            if (pkg != context.packageName) {
                val label = info.loadLabel(pm).toString()
                val icon = info.loadIcon(pm)
                appList.add(AppItem(label, pkg, icon))
            }
        }
        appList.sortBy { it.label.lowercase() }
    }

    private class AppAdapter(
        private val context: Context,
        private val items: List<AppItem>,
        private val onClick: (AppItem) -> Unit
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_app_grid, parent, false)

            val item = items[position]
            val ivIcon = view.findViewById<ImageView>(R.id.ivAppIcon)
            val tvLabel = view.findViewById<TextView>(R.id.tvAppLabel)

            ivIcon.setImageDrawable(item.icon)
            tvLabel.text = item.label

            view.setOnClickListener { onClick(item) }
            return view
        }
    }
}
