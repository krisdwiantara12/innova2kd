package com.innova.launcher2kd.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.innova.launcher2kd.R
import com.innova.launcher2kd.service.UpdateManager
import java.util.Locale

class UpdateDialog(
    context: Context,
    private val versionName: String,
    private val changelog: String,
    private val apkUrl: String,
    private val updateManager: UpdateManager
) : Dialog(context, R.style.DialogTheme) {

    private lateinit var llChangelogSection: LinearLayout
    private lateinit var llProgressSection: LinearLayout
    private lateinit var tvDownloadStatus: TextView
    private lateinit var pbDownloadProgress: ProgressBar
    private lateinit var tvDownloadBytes: TextView
    private lateinit var tvDownloadPercent: TextView
    private lateinit var tvDownloadError: TextView
    private lateinit var btnCancelDownload: Button
    private lateinit var btnRetryDownload: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_update)
        setCancelable(false)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.78).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        findViewById<TextView>(R.id.tvUpdateTitle).text = "Pembaruan Innova 2KD (v$versionName)"
        findViewById<TextView>(R.id.tvUpdateChangelog).text = changelog

        llChangelogSection = findViewById(R.id.llChangelogSection)
        llProgressSection = findViewById(R.id.llProgressSection)
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus)
        pbDownloadProgress = findViewById(R.id.pbDownloadProgress)
        tvDownloadBytes = findViewById(R.id.tvDownloadBytes)
        tvDownloadPercent = findViewById(R.id.tvDownloadPercent)
        tvDownloadError = findViewById(R.id.tvDownloadError)
        btnCancelDownload = findViewById(R.id.btnCancelDownload)
        btnRetryDownload = findViewById(R.id.btnRetryDownload)

        findViewById<Button>(R.id.btnUpdateLater).setOnClickListener {
            dismiss()
        }

        findViewById<Button>(R.id.btnUpdateNow).setOnClickListener {
            startDownloadProcess()
        }

        btnCancelDownload.setOnClickListener {
            updateManager.cancelDownload()
            dismiss()
        }

        btnRetryDownload.setOnClickListener {
            startDownloadProcess()
        }
    }

    private fun startDownloadProcess() {
        llChangelogSection.visibility = View.GONE
        llProgressSection.visibility = View.VISIBLE
        tvDownloadError.visibility = View.GONE
        btnRetryDownload.visibility = View.GONE
        tvDownloadStatus.text = "Menghubungkan ke GitHub..."
        pbDownloadProgress.progress = 0
        tvDownloadPercent.text = "0%"
        tvDownloadBytes.text = "0 MB / -- MB"

        updateManager.downloadApkDirect(
            apkUrl = apkUrl,
            versionName = versionName,
            onProgress = { percent, downloaded, total ->
                tvDownloadStatus.post {
                    if (percent >= 0) {
                        pbDownloadProgress.isIndeterminate = false
                        pbDownloadProgress.progress = percent
                        tvDownloadPercent.text = "$percent%"
                    } else {
                        pbDownloadProgress.isIndeterminate = true
                        tvDownloadPercent.text = "--%"
                    }

                    val dlMb = downloaded / (1024f * 1024f)
                    val totMb = if (total > 0) total / (1024f * 1024f) else 0f
                    tvDownloadBytes.text = if (totMb > 0f) {
                        String.format(Locale.US, "%.2f MB / %.2f MB", dlMb, totMb)
                    } else {
                        String.format(Locale.US, "%.2f MB", dlMb)
                    }
                    tvDownloadStatus.text = "Mengunduh pembaruan v$versionName..."
                }
            },
            onSuccess = { apkFile ->
                tvDownloadStatus.post {
                    pbDownloadProgress.progress = 100
                    tvDownloadPercent.text = "100%"
                    tvDownloadStatus.text = "✅ Berkas APK terverifikasi utuh! Membuka installer..."
                    tvDownloadStatus.postDelayed({
                        dismiss()
                        updateManager.installApk(apkFile)
                    }, 600)
                }
            },
            onError = { errorMsg ->
                tvDownloadStatus.post {
                    tvDownloadStatus.text = "❌ Gagal mengunduh"
                    tvDownloadError.text = errorMsg
                    tvDownloadError.visibility = View.VISIBLE
                    btnRetryDownload.visibility = View.VISIBLE
                }
            }
        )
    }
}
