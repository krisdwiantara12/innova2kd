# 📲 PANDUAN LENGKAP: CARA MEMASANG (INSTALL) KE HEADUNIT & SETUP UPDATE ONLINE

Dokumen ini menjelaskan cara memindahkan file APK ke Headunit Innova 2KD Anda dan bagaimana cara menyiapkan sistem pembaruan online (*Over-The-Air / OTA Update*) agar headunit Anda otomatis memunculkan notifikasi saat ada versi baru.

---

## BAGIAN 1: CARA MEMASANG APK KE HEADUNIT (INNOVA 2009)

Ada 2 cara termudah untuk memasang file APK ini ke Headunit mobil Anda:

### METODE A: MENGGUNAKAN FLASHDISK USB (Paling Praktis & Direkomendasikan)
1. **Salin File APK**:
   - Ambil flashdisk biasa (format FAT32 atau NTFS).
   - Salin file APK (`Innova2KD-Launcher.apk`) dari komputer ke dalam flashdisk.
2. **Colok ke Mobil**:
   - Tancapkan flashdisk ke kabel port USB headunit (biasanya kabelnya dijulurkan ke dalam laci dashboard / *glove box* atau *center console* Innova Anda).
3. **Buka File Manager di Headunit**:
   - Nyalakan kontak mobil ke posisi **ACC**.
   - Di layar headunit, cari dan buka aplikasi bawaan bernama **File Manager**, **ES File Explorer**, atau **ApkInstaller**.
   - Buka folder USB Drive / Flashdisk Anda.
4. **Izinkan Instalasi (*Unknown Sources*)**:
   - Sentuh file `Innova2KD-Launcher.apk`.
   - Jika muncul peringatan keamanan Android: *"Untuk keamanan, perangkat Anda tidak diizinkan memasang aplikasi dari sumber ini"*, klik **Pengaturan (Settings)** -> aktifkan tombol centang **"Izinkan dari Sumber Ini" (Allow Unknown Sources)**.
5. **Klik Pasang (Install)**:
   - Tunggu proses instalasi selesai (sekitar 3–5 detik).
6. **Jadikan Sebagai Home / Launcher Utama**:
   - Tekan tombol **HOME** fisik / sentuh di tepi headunit.
   - Akan muncul dialog pilihan: *"Pilih aplikasi untuk Beranda / Home"*:
     - Pilih **Innova 2KD Co-Pilot**.
     - Pilih **"Selalu" (Always)**.
   - Sekarang, setiap kali kontak mobil dinyalakan, tampilan kokpit Innova 2KD akan langsung muncul otomatis!

---

### METODE B: MENGGUNAKAN KONEKSI BLUETOOTH / GOOGLE DRIVE / BROWSER HU
Jika Anda malas cabut-colok flashdisk dan headunit sedang tersambung WiFi/Hotspot HP:
1. Buka browser (Google Chrome) di layar headunit.
2. Unduh langsung file APK dari link yang Anda simpan di Google Drive atau GitHub.
3. Buka menu unduhan (*Downloads*) di headunit dan klik pasang.

---

## BAGIAN 2: CARA KERJA UPDATE LEWAT INTERNET (OTA AUTO-UPDATE)

Aplikasi ini sudah ditanami fitur **OTA Update Manager**. Saat headunit Anda tersambung internet (misal saat Anda menyalakan Hotspot/Tethering dari HP saat berkendara), aplikasi akan otomatis mengecek apakah ada versi baru di internet.

### Di Mana Kodenya Ditaruh Agar Ada Notifikasi Update?

Tempat terbaik, paling stabil, gratis, dan tanpa perlu sewa hosting web adalah **GitHub Releases**:

```text
Alur Kerja Update:
[Laptop Anda: Buat APK Baru v1.0.1] 
       ⬇️ Upload ke
[GitHub Repo Anda (Gratis)]
       ⬇️ Headunit Konek Hotspot HP
[Headunit Mendeteksi version.json di Internet]
       ⬇️
[Layar Headunit Muncul Notifikasi: "Pembaruan Tersedia v1.0.1 - Unduh & Pasang"]
```

### Langkah Menyiapkan Tempat Update (Hanya Perlu 1 Kali):

#### 1. Repositori GitHub Resmi Anda
Repositori Anda telah ditentukan di:
👉 **[https://github.com/krisdwiantara12/innova2kd](https://github.com/krisdwiantara12/innova2kd)**

#### 2. Format File `version.json`
Di dalam proyek ini berkas [`version.json`](file:///f:/BOT/APK%20HEADUNIT/version.json) sudah disetel ke repositori Anda:
```json
{
  "versionCode": 1,
  "versionName": "1.0.0-OEM",
  "minAndroidVersion": 24,
  "releaseDate": "2026-09-03",
  "apkUrl": "https://github.com/krisdwiantara12/innova2kd/releases/download/v1.0.0/Innova2KD_CoPilot_v1.0.0.apk",
  "changelog": [
    "Rilis perdana Innova 2KD Co-Pilot & Launcher",
    "Speedometer GPS satelit presisi & anti flyover drop",
    "Audio DSP 10-Band EQ & preset akustik mesin diesel",
    "Pemantau voltase aki 12V dan deteksi cranking drop",
    "Diagram interaktif sekring darurat Innova 2009",
    "Tombol Cek Pembaruan & Sistem OTA Auto-Update GitHub"
  ],
  "forceUpdate": false
}
```

#### 3. Setiap Kali Ada Pembaruan Versi Baru:
1. Naikkan `versionCode` dan `versionName` di `app/build.gradle.kts` dan `version.json`.
2. Kompilasi APK baru.
3. Buka GitHub repositori Anda: `https://github.com/krisdwiantara12/innova2kd/releases` -> klik **Draft a new release**:
   - Tag: `v1.0.1` (sesuai versi)
   - Judul Release: `Innova 2KD Co-Pilot v1.0.1`
   - Lampirkan / Upload file APK baru (`Innova2KD_CoPilot_v1.0.1.apk`).
4. Commit & push file `version.json` yang baru ke branch `main`.
5. Di Headunit mobil Anda, buka **Pengaturan** -> klik tombol **🔄 CEK UPDATE**, atau biarkan aplikasi mendeteksi otomatis saat menyala!

#### 4. Apa yang Terjadi di Headunit Mobil?
- Saat Anda menyalakan hotspot HP di dalam mobil, aplikasi akan membaca file `version.json` tersebut.
- Layar headunit akan otomatis menampilkan jendela pop-up OEM elegan:
  > **Pembaruan Sistem Tersedia (v1.0.1)**  
  > *Catatan Perubahan:*  
  > • Peningkatan akurasi speedometer GPS di jalan tol  
  > • Penambahan filter audio suara jernih Innova 2KD  
  > **[UNDUH & PASANG]**
- Pengemudi cukup menekan tombol **UNDUH & PASANG**. File APK akan diunduh otomatis dan jendela instalasi langsung terbuka tanpa perlu mencolok flashdisk lagi!
