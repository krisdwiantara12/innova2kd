package com.innova.launcher2kd.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class UpdateManager(
    private val context: Context,
    private val onUpdateAvailable: (versionName: String, changelog: String, apkUrl: String) -> Unit,
    private val onNoUpdate: () -> Unit = {}
) {
    companion object {
        // Repositori GitHub resmi milik krisdwiantara12
        const val DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/krisdwiantara12/innova2kd/main/version.json"
    }

    fun checkForUpdates(
        customUrl: String = DEFAULT_UPDATE_URL,
        isManual: Boolean = false,
        onManualResult: ((hasUpdate: Boolean, message: String) -> Unit)? = null
    ) {
        thread {
            try {
                val url = URL(customUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.requestMethod = "GET"

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val jsonStr = reader.use { it.readText() }
                    val jsonObj = JSONObject(jsonStr)

                    val remoteVersionCode = jsonObj.optInt("versionCode", 1)
                    val remoteVersionName = jsonObj.optString("versionName", "1.0.0")
                    val apkUrl = jsonObj.optString("apkUrl", "")
                    
                    val changelogArr = jsonObj.optJSONArray("changelog")
                    val changelogBuilder = StringBuilder()
                    if (changelogArr != null) {
                        for (i in 0 until changelogArr.length()) {
                            changelogBuilder.append("• ").append(changelogArr.getString(i)).append("\n")
                        }
                    }
                    val changelog = changelogBuilder.toString().trim()

                    val currentVersionCode = getLocalVersionCode()

                    if (remoteVersionCode > currentVersionCode && apkUrl.isNotEmpty()) {
                        onUpdateAvailable(remoteVersionName, changelog, apkUrl)
                        onManualResult?.invoke(true, "Versi baru v$remoteVersionName ditemukan!")
                    } else {
                        onNoUpdate()
                        onManualResult?.invoke(false, "Aplikasi sudah versi terbaru (v${getLocalVersionName()})")
                    }
                } else {
                    onNoUpdate()
                    onManualResult?.invoke(false, "Tidak dapat menghubungi server update (HTTP ${conn.responseCode})")
                }
            } catch (e: Exception) {
                onNoUpdate()
                onManualResult?.invoke(false, "Pengecekan gagal. Pastikan Headunit terhubung ke internet/hotspot HP.")
            }
        }
    }

    fun getLocalVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun getLocalVersionCode(): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    fun downloadAndInstall(apkUrl: String, versionName: String) {
        val fileName = "Innova2KD_v$versionName.apk"
        val destFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destFile.exists()) {
            destFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Mengunduh Pembaruan Innova 2KD")
            setDescription("Versi $versionName sedang diunduh...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Daftarkan receiver saat unduhan selesai untuk memicu instalasi otomatis
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    installApk(fileName)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(fileName: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists() || file.length() < 1_000_000) {
            // Berkas belum lengkap atau korup
            return
        }

        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Berikan izin baca eksplisit ke package installer Android
        val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resInfoList) {
            val pkg = resolveInfo.activityInfo.packageName
            context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    }
}
