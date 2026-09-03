package com.innova.launcher2kd.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Robust OTA Update Manager for Android Headunits
 * Mengunduh langsung via Direct HTTP Streaming (bypass DownloadManager yang sering rusak di HU Cina).
 * Dilengkapi verifikasi keutuhan berkas (pre-parse package check) sebelum membuka installer
 * untuk mencegah 100% error "Terjadi kesalahan saat mengurai paket".
 */
class UpdateManager(
    private val context: Context,
    private val onUpdateAvailable: (versionName: String, changelog: String, apkUrl: String) -> Unit,
    private val onNoUpdate: () -> Unit = {}
) {
    companion object {
        const val DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/krisdwiantara12/innova2kd/main/version.json"
    }

    @Volatile
    private var isDownloadCancelled = false

    fun cancelDownload() {
        isDownloadCancelled = true
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
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Innova2KD-Headunit-OTA")

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
                    onManualResult?.invoke(false, "Server update merespons HTTP ${conn.responseCode}")
                }
            } catch (e: Exception) {
                onNoUpdate()
                onManualResult?.invoke(false, "Gagal memeriksa update. Pastikan koneksi internet aktif.")
            }
        }
    }

    fun downloadApkDirect(
        apkUrl: String,
        versionName: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit,
        onSuccess: (apkFile: File) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        isDownloadCancelled = false

        thread {
            var currentUrl = apkUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0

            try {
                // Loop handling redirect HTTP 301, 302, 307, 308 (GitHub raw to CDN redirect)
                while (redirectCount < 6) {
                    val url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Innova2KD-OTA")
                        setRequestProperty("Accept-Encoding", "identity")
                    }

                    val code = connection.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                        code == HttpURLConnection.HTTP_MOVED_TEMP ||
                        code == HttpURLConnection.HTTP_SEE_OTHER ||
                        code == 307 || code == 308
                    ) {
                        val newLocation = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (!newLocation.isNullOrEmpty()) {
                            currentUrl = newLocation
                            redirectCount++
                            continue
                        }
                    }
                    break
                }

                val conn = connection ?: throw IllegalStateException("Koneksi gagal dibuat")
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("Server merespons status HTTP ${conn.responseCode}")
                }

                val totalLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    conn.contentLengthLong.let { if (it > 0) it else conn.contentLength.toLong() }
                } else {
                    conn.contentLength.toLong()
                }

                // Simpan ke direktori app files internal (selalu aman tanpa masalah permission Android 10/11)
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val targetFile = File(downloadDir, "Innova2KD_v$versionName.apk")
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead: Long = 0
                        var lastProgressUpdateMs = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (isDownloadCancelled) {
                                targetFile.delete()
                                onError("Unduhan dibatalkan oleh pengguna.")
                                return@thread
                            }

                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdateMs > 120 || totalRead == totalLength) {
                                lastProgressUpdateMs = now
                                val percent = if (totalLength > 0) ((totalRead * 100) / totalLength).toInt() else -1
                                onProgress(percent, totalRead, totalLength)
                            }
                        }
                        output.flush()
                    }
                }

                // Buat berkas terbaca oleh installer sistem
                targetFile.setReadable(true, false)

                // 🛡️ AUDIT INTEGRITAS BERKAS: Pre-Parse Package Check
                // Pastikan berkas benar-benar APK utuh sebelum membuka installer
                val archiveInfo = context.packageManager.getPackageArchiveInfo(targetFile.absolutePath, 0)
                if (archiveInfo == null) {
                    targetFile.delete()
                    onError("Berkas unduhan korup atau terpotong (Gagal verifikasi APK). Silakan coba lagi.")
                    return@thread
                }

                if (archiveInfo.packageName != context.packageName) {
                    targetFile.delete()
                    onError("Paket APK tidak valid (${archiveInfo.packageName}). Unduhan dibatalkan.")
                    return@thread
                }

                // Berkas 100% valid dan siap pasang!
                onSuccess(targetFile)

            } catch (e: Exception) {
                if (isDownloadCancelled) {
                    onError("Unduhan dibatalkan.")
                } else {
                    onError("Gagal mengunduh: ${e.localizedMessage ?: "Koneksi terputus"}")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun installApk(file: File) {
        if (!file.exists()) return

        // 1. Validasi ulang sebelum trigger
        val pi = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        if (pi == null) {
            Toast.makeText(context, "Berkas APK tidak lengkap atau rusak.", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Izin Install Unknown Apps (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "Harap izinkan pemasangan aplikasi tidak dikenal.", Toast.LENGTH_LONG).show()
                try {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                } catch (e: Exception) {}
            }
        }

        // 3. Bangun Intent Instalasi dengan FileProvider
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val apkUri: Uri = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")

        // Berikan izin eksplisit ke seluruh package installer yang tersedia di sistem
        try {
            val resolveList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resolveList) {
                val pkg = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {}

        // 4. Buka Penginstal
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback untuk Custom ROM Headunit Cina yang memblokir FileProvider
            try {
                val builder = StrictMode.VmPolicy.Builder()
                StrictMode.setVmPolicy(builder.build())
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Gagal meluncurkan installer: ${e2.message}", Toast.LENGTH_LONG).show()
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
}
