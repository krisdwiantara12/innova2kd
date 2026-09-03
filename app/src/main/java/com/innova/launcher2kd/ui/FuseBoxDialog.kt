package com.innova.launcher2kd.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.innova.launcher2kd.R
import com.innova.launcher2kd.model.FuseItem

class FuseBoxDialog(context: Context) : Dialog(context, R.style.DialogTheme) {

    private val engineFuses = listOf(
        FuseItem("EFI", "25A", "Komputer ECU 2KD & Common Rail Supply Pump", "ENGINE"),
        FuseItem("GLOW", "80A", "Busi Pijar Pemanas Mesin Diesel (Glow Plug)", "ENGINE"),
        FuseItem("ALT", "140A", "Alternator Utama Pengisian Aki", "ENGINE"),
        FuseItem("HORN", "10A", "Klakson Mobil", "ENGINE"),
        FuseItem("HEAD HI", "20A", "Lampu Jauh (High Beam)", "ENGINE"),
        FuseItem("HEAD LO", "15A", "Lampu Dekat (Low Beam)", "ENGINE"),
        FuseItem("AM2", "30A", "Sistem Kunci Kontak Utama Starter", "ENGINE"),
        FuseItem("COND FAN", "30A", "Ekstra Fan Kondensor AC", "ENGINE"),
        FuseItem("FOG", "15A", "Lampu Kabut (Foglamp)", "ENGINE")
    )

    private val cabinFuses = listOf(
        FuseItem("CIG", "15A", "Soket Lighter 12V / Colokan Charger HP", "CABIN"),
        FuseItem("ACC", "7.5A", "Arus Kontak Headunit & Spion Elektrik", "CABIN"),
        FuseItem("POWER", "30A", "Motor Power Window Kaca Pintu", "CABIN"),
        FuseItem("WIPER", "20A", "Wiper Kaca Depan & Belakang", "CABIN"),
        FuseItem("GAUGE", "10A", "Panel Spidometer & Indikator Dashboard", "CABIN"),
        FuseItem("ECU-IG", "10A", "Sistem Sensor ABS & Modul Airbag", "CABIN"),
        FuseItem("STOP", "10A", "Lampu Rem Belakang", "CABIN"),
        FuseItem("DOME", "10A", "Lampu Plafon Kabin & Jam Tengah", "CABIN"),
        FuseItem("TAIL", "10A", "Lampu Senja/Kota & Lampu Plat Nomor", "CABIN"),
        FuseItem("OBD", "7.5A", "Soket Diagnostik DLC3 Bawah Setir", "CABIN")
    )

    private var currentList = engineFuses

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_fuse_box)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.90).toInt()
        )

        val btnEngine = findViewById<Button>(R.id.btnFuseEngine)
        val btnCabin = findViewById<Button>(R.id.btnFuseCabin)
        val lvFuses = findViewById<ListView>(R.id.lvFuses)
        val btnClose = findViewById<Button>(R.id.btnCloseFuse)

        val adapter = FuseAdapter(context, currentList)
        lvFuses.adapter = adapter

        btnEngine.setOnClickListener {
            currentList = engineFuses
            adapter.updateItems(currentList)
            btnEngine.setBackgroundColor(ContextCompat.getColor(context, R.color.accent_amber))
            btnEngine.setTextColor(ContextCompat.getColor(context, R.color.text_inverse))
            btnCabin.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            btnCabin.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }

        btnCabin.setOnClickListener {
            currentList = cabinFuses
            adapter.updateItems(currentList)
            btnCabin.setBackgroundColor(ContextCompat.getColor(context, R.color.accent_amber))
            btnCabin.setTextColor(ContextCompat.getColor(context, R.color.text_inverse))
            btnEngine.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            btnEngine.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }

        btnClose.setOnClickListener { dismiss() }
    }

    private class FuseAdapter(
        private val context: Context,
        private var items: List<FuseItem>
    ) : BaseAdapter() {

        fun updateItems(newItems: List<FuseItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)

            val item = items[position]
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)

            text1.text = "${item.code}  [ ${item.ampere} ]"
            text1.setTextColor(ContextCompat.getColor(context, R.color.accent_amber))
            text1.textSize = 13f

            text2.text = item.description
            text2.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            text2.textSize = 11f

            return view
        }
    }
}
