# 🤖 AGENTIC AI INSTRUCTIONS & VERSIONING PROTOCOL
> **IMPORTANT FOR ALL AI CODING AGENTS (Antigravity, Cursor, Copilot, Claude, etc.):**
> You MUST read and strictly enforce every rule in this document whenever you inspect, modify, or add any code in this repository.

---

## 🚨 MANDATORY LAW: AUTOMATIC VERSION BUMP ON ANY CODE CHANGE

**Setiap kali Anda (AI Agent) melakukan modifikasi kode sekecil apa pun di proyek ini—termasuk perbaikan bug 1 baris, perubahan warna/UI, perbaikan teks, hingga penambahan fitur baru—Anda WAJIB menaikkan nomor versi aplikasi dan memperbarui file rilis.**

Pengguna memasang aplikasi ini di Headunit mobil dan mengandalkan sistem **OTA Auto-Update via GitHub**. Jika Anda lupa menaikkan versi, Headunit pengguna **tidak akan pernah mendeteksi pembaruan**.

---

### 📋 1. Aturan Penomoran Versi (Semantic Versioning)

Lokasi berkas versi:
- [`app/build.gradle.kts`](file:///f:/BOT/APK%20HEADUNIT/app/build.gradle.kts) (`versionCode` & `versionName`)
- [`version.json`](file:///f:/BOT/APK%20HEADUNIT/version.json) (`versionCode`, `versionName`, `apkUrl`, `changelog`, `releaseDate`)

Pilih kenaikan versi sesuai besaran perubahan:

1. **PATCH (Perubahan Kecil / Bug Fix)**:
   - *Kriteria*: Perbaikan bug, perbaikan typo/string, penyesuaian margin/padding UI, perbaikan error kecil.
   - *Aturan*: `versionCode` bertambah `+1`, `versionName` naik di digit terakhir.
   - *Contoh*: `1.0.0-OEM` ➔ `1.0.1-OEM`.

2. **MINOR (Fitur Baru / Peningkatan Menengah)**:
   - *Kriteria*: Penambahan fitur baru (misal: preset audio baru, sensor baru, halaman baru, integrasi Bluetooth).
   - *Aturan*: `versionCode` bertambah `+1`, `versionName` naik di digit tengah dan digit terakhir reset ke 0.
   - *Contoh*: `1.0.1-OEM` ➔ `1.1.0-OEM`.

3. **MAJOR (Perombakan Besar / Rombak Desain Total)**:
   - *Kriteria*: Perombakan arsitektur besar, perubahan UI total, pergantian basis sistem.
   - *Aturan*: `versionCode` bertambah `+1`, `versionName` naik di digit depan.
   - *Contoh*: `1.2.5-OEM` ➔ `2.0.0-OEM`.

---

### ⚙️ 2. Checklist Wajib AI Saat Menyelesaikan Tugas

Sebelum melaporkan pekerjaan selesai kepada user, AI WAJIB menjalankan tahapan ini:

1. [ ] **Update `app/build.gradle.kts`**:
   - Naikkan `versionCode` (integer).
   - Naikkan `versionName` (string).
2. [ ] **Update `version.json`**:
   - Samakan `versionCode` dan `versionName`.
   - Perbarui `apkUrl` sesuai tag versi baru:
     `https://github.com/krisdwiantara12/innova2kd/releases/download/vX.Y.Z/Innova2KD_CoPilot_vX.Y.Z.apk`
   - Tuliskan poin-poin perubahan pada array `"changelog"`.
   - Perbarui tanggal rilis (`"releaseDate": "YYYY-MM-DD"`).
3. [ ] **Kompilasi Ulang APK**:
   - Jalankan perintah build:
     ```powershell
     $env:JAVA_HOME = "F:\BOT\jdk-17\jdk-17.0.20.1+1";
     $env:ANDROID_HOME = "F:\BOT\android-sdk";
     .\gradlew.bat assembleDebug --console=plain
     ```
4. [ ] **Salin Hasil APK ke Folder Rilis**:
   - Salin file APK baru ke folder `RELEASE_APK`:
     ```powershell
     Copy-Item "app\build\outputs\apk\debug\app-debug.apk" "RELEASE_APK\Innova2KD_CoPilot_vX.Y.Z.apk"
     ```
5. [ ] **Instruksikan Pengguna**:
   - Beritahu pengguna versi baru yang dihasilkan.
   - Berikan instruksi git commit, git tag, dan upload file APK baru ke GitHub Releases agar headunit mobil dapat langsung mendownloadnya via OTA.
