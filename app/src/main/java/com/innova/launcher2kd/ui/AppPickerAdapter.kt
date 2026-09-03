package com.innova.launcher2kd.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.innova.launcher2kd.R
import com.innova.launcher2kd.model.AppItem
import java.util.Locale

class AppPickerAdapter(
    private val allApps: List<AppItem>,
    private val onAppSelected: (AppItem) -> Unit
) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

    private var filteredApps: MutableList<AppItem> = allApps.toMutableList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivPickerAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvPickerAppName)
        val tvPkg: TextView = view.findViewById(R.id.tvPickerAppPkg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.tvName.text = app.label
        holder.tvPkg.text = app.packageName
        if (app.icon != null) {
            holder.ivIcon.setImageDrawable(app.icon)
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_apps_grid)
        }

        holder.itemView.setOnClickListener {
            onAppSelected(app)
        }
    }

    override fun getItemCount(): Int = filteredApps.size

    fun filter(query: String) {
        filteredApps.clear()
        if (query.trim().isEmpty()) {
            filteredApps.addAll(allApps)
        } else {
            val q = query.lowercase(Locale.getDefault())
            for (app in allApps) {
                if (app.label.lowercase(Locale.getDefault()).contains(q) ||
                    app.packageName.lowercase(Locale.getDefault()).contains(q)) {
                    filteredApps.add(app)
                }
            }
        }
        notifyDataSetChanged()
    }
}
