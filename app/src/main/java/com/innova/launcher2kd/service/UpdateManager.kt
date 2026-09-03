package com.innova.launcher2kd.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
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
 * Robust OTA Update Manager for Android Headunits (v3 - Definitive Fix)
 *
 * TIGA AKAR MASALAH "Terjadi kesalahan saat mengurai paket" yang ditangani:
 *
 * 1. SIGNATURE MISMATCH: APK debug ditandatangani dengan keystore berbeda dari APK yang
 *    sudah terpasang. Android menolak upgrade jika sertifikat berbeda.
 *    → Solusi: Deteksi mismatch, tawarkan uninstall-then-install otomatis.
 *
 * 2. FILE TERPOTONG / CORRUPT: Download dari GitHub CDN bisa terpotong di tengah jalan.
 *    → Solusi: Verifikasi Content-Length vs byte tertulis + PackageManager.getPackageArchiveInfo()
 *
 * 3. FILEPROVIDER GAGAL DI HU CINA: Custom ROM Headunit Cina sering memblokir content:// URI.
 *    → Solusi: Multi-layer fallback: FileProvider → StrictMode bypass → direct file URI
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
                // ═══════════════════════════════════════════════════════════════
                // FASE 1: RESOLUSI REDIRECT (GitHub raw → CDN → final URL)
                // ═══════════════════════════════════════════════════════════════
                while (redirectCount < 8) {
                    val url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false  // Handle redirect manual agar bisa hitung hop
                        connectTimeout = 20000
                        readTimeout = 60000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}) Innova2KD-OTA/3.0")
                        setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                        setRequestProperty("Accept-Encoding", "identity")  // Jangan compress, kita butuh raw bytes
                        setRequestProperty("Connection", "keep-alive")
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
                            currentUrl = if (newLocation.startsWith("http")) {
                                newLocation
                            } else {
                                // Handle relative redirect
                                val base = URL(currentUrl)
                                URL(base, newLocation).toString()
                            }
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

                // ═══════════════════════════════════════════════════════════════
                // FASE 2: DOWNLOAD DENGAN VERIFIKASI BYTE-LEVEL
                // ═══════════════════════════════════════════════════════════════
                val contentLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    conn.contentLengthLong.let { if (it > 0) it else conn.contentLength.toLong() }
                } else {
                    conn.contentLength.toLong()
                }

                // Validasi: APK harus minimal 100KB (APK kosong saja ~50KB)
                if (contentLength in 1..102400) {
                    throw IllegalStateException("Ukuran file terlalu kecil ($contentLength bytes). Kemungkinan bukan APK valid.")
                }

                // Simpan ke direktori app files internal (bebas Scoped Storage restriction)
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val targetFile = File(downloadDir, "Innova2KD_v$versionName.apk")

                // Bersihkan file lama jika ada
                if (targetFile.exists()) targetFile.delete()

                // Bersihkan juga semua APK update lama untuk hemat storage 32GB
                downloadDir.listFiles()?.filter {
                    it.name.startsWith("Innova2KD_v") && it.name.endsWith(".apk") && it.name != targetFile.name
                }?.forEach { it.delete() }

                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16384) // 16KB buffer untuk download lebih cepat
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
                            if (now - lastProgressUpdateMs > 150 || totalRead == contentLength) {
                                lastProgressUpdateMs = now
                                val percent = if (contentLength > 0) ((totalRead * 100) / contentLength).toInt() else -1
                                onProgress(percent, totalRead, contentLength)
                            }
                        }
                        output.flush()
                        output.fd.sync() // Force flush ke disk fisik — PENTING untuk eMMC HU!
                    }
                }

                // Buat berkas terbaca oleh installer sistem
                targetFile.setReadable(true, false)

                // ═══════════════════════════════════════════════════════════════
                // FASE 3: TRIPLE INTEGRITY VERIFICATION
                // ═══════════════════════════════════════════════════════════════

                // Check 1: Verifikasi ukuran file vs Content-Length
                val actualFileSize = targetFile.length()
                if (contentLength > 0 && actualFileSize != contentLength) {
                    targetFile.delete()
                    onError("Download tidak lengkap! Terunduh: ${actualFileSize / 1024}KB, dibutuhkan: ${contentLength / 1024}KB. Silakan coba lagi.")
                    return@thread
                }

                // Check 2: Verifikasi APK bisa di-parse oleh PackageManager
                val archiveInfo: PackageInfo? = try {
                    context.packageManager.getPackageArchiveInfo(targetFile.absolutePath, 0)
                } catch (e: Exception) {
                    null
                }

                if (archiveInfo == null) {
                    targetFile.delete()
                    onError("Berkas unduhan korup atau terpotong (Gagal verifikasi APK). Silakan coba lagi.")
                    return@thread
                }

                // Check 3: Verifikasi package name cocok
                if (archiveInfo.packageName != context.packageName) {
                    targetFile.delete()
                    onError("Paket APK tidak valid (${archiveInfo.packageName}). Unduhan dibatalkan.")
                    return@thread
                }

                // Check 4: Verifikasi ukuran file masuk akal (minimal 500KB)
                if (actualFileSize < 512000) {
                    targetFile.delete()
                    onError("Ukuran file APK tidak wajar (${actualFileSize / 1024}KB). Kemungkinan terkorupsi.")
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

    /**
     * Cek apakah APK yang akan di-install memiliki signing certificate yang sama
     * dengan APK yang sudah terpasang. Jika berbeda, Android PASTI menolak upgrade
     * dengan error "Terjadi kesalahan saat mengurai paket".
     */
    private fun isSignatureMismatch(newApkPath: String): Boolean {
        return try {
            val installedSigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pi = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                pi.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val pi = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                pi.signatures
            }

            val newApkSigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pi = context.packageManager.getPackageArchiveInfo(
                    newApkPath,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                pi?.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val pi = context.packageManager.getPackageArchiveInfo(
                    newApkPath,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                pi?.signatures
            }

            if (installedSigs == null || newApkSigs == null) {
                return false // Jika tidak bisa baca, coba saja install
            }

            // Bandingkan byte array signature pertama
            val installedBytes = installedSigs[0].toByteArray()
            val newBytes = newApkSigs[0].toByteArray()

            !installedBytes.contentEquals(newBytes)
        } catch (e: Exception) {
            false // Jika gagal cek, biarkan installer yang handle
        }
    }

    fun installApk(file: File) {
        if (!file.exists()) return

        // 1. Validasi ulang sebelum trigger
        val pi = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        if (pi == null) {
            Toast.makeText(context, "Berkas APK tidak lengkap atau rusak. Download ulang diperlukan.", Toast.LENGTH_LONG).show()
            file.delete()
            return
        }

        // 2. Deteksi Signature Mismatch (AKAR MASALAH UTAMA "mengurai paket")
        if (isSignatureMismatch(file.absolutePath)) {
            // Signature berbeda! Android PASTI menolak upgrade.
            // Solusinya: Uninstall dulu, lalu install baru.
            Toast.makeText(
                context,
                "Memperbarui dengan sertifikat baru. Aplikasi akan di-uninstall lalu dipasang ulang...",
                Toast.LENGTH_LONG
            ).show()

            // Simpan path APK untuk diinstall setelah uninstall
            context.getSharedPreferences("ota_update", Context.MODE_PRIVATE)
                .edit()
                .putString("pending_install_path", file.absolutePath)
                .commit()

            // Trigger uninstall
            try {
                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(uninstallIntent)
            } catch (e: Exception) {
                // Jika uninstall gagal, coba install langsung (mungkin berhasil)
                triggerInstallIntent(file)
            }
            return
        }

        // 3. Izin Install Unknown Apps (Android 8.0+)
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
                // Jangan return — coba install anyway, beberapa HU Cina tidak butuh izin ini
            }
        }

        // 4. Trigger Install Intent (dengan multi-layer fallback)
        triggerInstallIntent(file)
    }

    /**
     * Multi-Layer Install Intent Strategy untuk kompatibilitas Headunit Cina:
     *
     * Layer 1: FileProvider content:// URI (standar Android 7+)
     * Layer 2: StrictMode bypass + file:// URI langsung (untuk HU Cina tanpa FileProvider support)
     * Layer 3: ACTION_INSTALL_PACKAGE dengan direct file URI (Android lama)
     */
    private fun triggerInstallIntent(file: File) {
        // === LAYER 1: FileProvider content:// URI (standar) ===
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Grant URI permission ke semua package installer di sistem
            val resolveList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resolveList) {
                val pkg = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            return // Berhasil!
        } catch (e: Exception) {
            // FileProvider gagal, coba layer berikutnya
        }

        // === LAYER 2: StrictMode bypass + file:// URI langsung ===
        try {
            // Bypass StrictMode FileUriExposedException
            val oldPolicy = StrictMode.getVmPolicy()
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // Restore policy setelah 5 detik
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                StrictMode.setVmPolicy(oldPolicy)
            }, 5000)
            return // Berhasil!
        } catch (e: Exception) {
            // Layer 2 gagal, coba layer terakhir
        }

        // === LAYER 3: ACTION_INSTALL_PACKAGE (deprecated tapi masih work di HU Cina) ===
        try {
            @Suppress("DEPRECATION")
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, false)
            }
            context.startActivity(intent)
            return // Berhasil!
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Gagal membuka installer. Pasang manual dari: ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
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
