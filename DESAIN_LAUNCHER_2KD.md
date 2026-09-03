# 🚗 DOKUMEN RANCANGAN SPESIFIKASI: INNOVA 2KD OEM-GRADE CO-PILOT LAUNCHER
> **Versi Dokumen**: 2.0 (Tahap Perancangan Matang)  
> **Target Perangkat**: Headunit Android Universal (Toyota Kijang Innova 2009 2KD-FTV)  
> **Standar Kualitas**: **OEM Factory-Grade (Standar Pabrikan Otomotif)**, **100% Offline (Tanpa Internet)**, **Ultra Lightweight (< 30 MB RAM)**, **Fluid 60 FPS**.

---

## 1. JAWABAN PERTANYAAN FUNDAMENTAL: APAKAH MEMBUTUHKAN INTERNET?

### **Jawab: TIDAK SAMA SEKALI (100% Offline)**
Aplikasi ini dirancang beroperasi penuh secara lokal tanpa memerlukan kartu SIM, WiFi, ataupun kuota internet:
1. **Speedometer & Altimeter**: Bekerja menggunakan sinyal radio satelit langsung ke antena GPS headunit (gratis dari satelit GPS/GLONASS, tidak butuh sinyal seluler).
2. **Maintenance Tracker & Odometer**: Seluruh perhitungan jarak, riwayat ganti oli, dan filter solar dihitung dan disimpan di memori internal headunit.
3. **Database Rest Area & Fitur Bawaan**: Tersimpan *embedded* di dalam aplikasi.
4. **Keuntungan untuk Mobil**:
   - Langsung aktif seketika saat mobil distarter di basement, parkiran bawah tanah gedung, ataupun di pelosok pedalaman tanpa sinyal HP.
   - Tidak ada proses sync latar belakang (*background fetch*) yang membebani CPU/RAM headunit.

---

## 2. STANDAR DESAIN PABRIKAN (OEM AUTOMOTIVE DESIGN SYSTEM)
Aplikasi otomotif buatan pabrikan (seperti Toyota Touch 2, Lexus Enform, atau sistem Alpine/Pioneer premium) memiliki standar visual yang sangat berbeda dari aplikasi HP biasa:

### A. Filosofi Visual: "Lexus-Clean & Toyota Gazoo Heritage"
* **Bukan Tampilan Abal-Abal**: Menghindari gradasi warna murah, ikon kartun, atau teks kecil bertumpuk.
* **Warna Dasar (Background)**: *Deep Obsidian Carbon* (`#0B0E14` & `#141822`) — hitam pekat elegan yang menyatu dengan bingkai bezel headunit, tidak menyilaukan mata saat berkendara malam hari.
* **Pilihan Aksen Warna (Color Theme)**:
  1. **Innova Classic Amber (`#FF8C00` / `#FFA726`)**: Senada persis dengan pancaran lampu instrumen dashboard & panel AC analog Innova 2009, memberikan kesan *original factory look*.
  2. **Modern Cyber Cyan (`#00D2FF`)**: Tampilan modern seperti instrumen kokpit mobil hybrid kekinian.
  3. **Stealth Monokrom & Silver (`#E0E6ED`)**: Nuansa mewah, simpel, dan elegan ala mobil Eropa.
* **Tipografi Tabular Anti-Jitter**: Menggunakan font khusus angka otomotif bertipe *tabular monospace* (seperti *Rajdhani* atau *Chakra Petch*). Angka kecepatan tidak akan bergoyang atau menggeser layout saat angkanya berubah dari 9 ke 10 atau 99 ke 100.
* **Bezel & Card Kontur**: Garis aksen tipis 1px *subtle metallic border* dengan radius sudut modern (12px), menciptakan ilusi instrumen fisik yang terintegrasi rapi.

---

## 3. STRUKTUR TATA LETAK LAYAR UTAMA (MAIN COCKPIT)

Rasio Layar Landscape (1024x600 / 1280x720) dengan proporsi ergonomis jangkauan tangan pengemudi:

```text
+---------------------------------------------------------------------------------------------------------+
| [TOP BAR]  GPS: LOCKED [8 Sat]  |  AKI: 13.8V  |  ODO: 142.350 KM  |  SUHU: 29°C  |  12:45  | [SCREEN OFF]|
+--------------------------------------+----------------------------+-------------------------------------+
|                                      |                            |                                     |
|           SPEEDOMETER GAUGE          |    DYNAMIC SENTINEL CARD   |        QUICK APP SHORTCUTS          |
|                                      |  (Bisa Di-Slide / Auto)    |     (3 Slot Besar Kustomisasi)      |
|                 85                   |                            |                                     |
|                KM/H                  |   [ CARD 1: SERVIS 2KD ]   |     +-------------------------+     |
|                                      |   - Oli Mesin: 3.200 km    |     |   🗺️  GOOGLE MAPS        |     |
|   SPEED LIMIT: 100 km/h (ACTIVE)     |   - Filter Solar: 7.400 km |     +-------------------------+     |
|                                      |   - Status: SEHAT          |     |   🎵  SPOTIFY / MUSIK   |     |
|   ARAH   : UTARA (350°)              |                            |     +-------------------------+     |
|   ELEVASI: 420 MDPL (Tanjakan)       |   [ CARD 2: TRIP & SOLAR ] |     |   📻  RADIO / BLUETOOTH |     |
|   SESSION: 01:24:10                  |   - Jarak : 45.2 km        |     +-------------------------+     |
|                                      |   - Est. BBM: 4.1 Liter    |     |   [ ::: ALL APPS ::: ]  |     |
|                                      |                            |     +-------------------------+     |
+--------------------------------------+----------------------------+-------------------------------------+
| [BOTTOM BAR]  Vol - [ ============||======== ] Vol +   |   [ MUTE ]   |   [ BRIGHTNESS ]   |  [ SETTINGS ]  |
+---------------------------------------------------------------------------------------------------------+
```

---

## 4. DETAIL FITUR-FITUR INTI

### A. Tiga Shortcut Aplikasi (Quick App Slots) + Tombol All Apps
1. **3 Slot Utama (Driver Ergonomics)**:
   - Tombol dibuat berukuran besar (tinggi minimal 65-75 dp) dengan feedback sentuhan haptik/visual halus.
   - **Customizable**: Cukup tekan dan tahan (*long-press*) selama 1.5 detik pada slot mana saja untuk membuka popup pilihan aplikasi yang ingin dipasang di slot tersebut.
   - Di menu Pengaturan, user tetap bisa memilih layout: **3 Slot (Rekomendasi Terbaik)**, **4 Slot**, atau **5 Slot**.
2. **Tombol "ALL APPS" (App Drawer Standar Pabrik)**:
   - Terletak tepat di bawah 3 slot utama dengan label jelas `[ ::: ALL APPS ::: ]`.
   - Sekali sentuh, membuka **Fullscreen Automotive App Drawer**:
     - Ditata dalam bentuk **Grid Rapi (misal: 4 kolom x 2 baris)** per halaman.
     - Paginasi geser kiri-kanan yang sangat mulus (smooth swipe pagination).
     - Tombol kembali instan `[ X ]` di pojok kanan atas.
     - Opsi cerdas: Bisa menyembunyikan (*hide*) aplikasi sistem yang tidak penting (misal: *Factory Test, Android Settings bawaan Cina, dsb.*) sehingga hanya menampilkan aplikasi yang sering digunakan.

### B. Speedometer & Warning System (Zero-Lag GPS)
1. **Speedometer Presisi Tinggi & Inertial Smoothing**:
   - Filter algoritma *Low-Pass Filter* pada data GPS agar pergerakan jarum/angka mulus, tidak patah-patah (*jitter-free*).
   - **Flyover & Tunnel Anti-Drop**: Saat mobil melewati kolong jalan layang (flyover/terowongan) dan sinyal GPS terputus 1-3 detik, angka kecepatan tidak akan anjlok mendadak ke 0 km/jam. Sistem menahan angka terakhir secara halus sebelum transisi.
2. **Speed Warning Alert (Anti-Tilang Tol)**:
   - Pengaturan batas kecepatan (misal 100 km/jam).
   - Saat terlampaui: Angka berubah menjadi aksen merah menyala dan membunyikan nada lonceng (*chime*) elegan 2 ketukan (bukan bunyi alarm bising yang mengagetkan).

### C. Smart Context-Aware Card (Menggantikan Swipe Manual Demi Keselamatan)
Untuk mencegah pengemudi terdistraksi menggeser layar saat kecepatan tinggi, kartu informasi tengah berganti **secara otomatis sesuai konteks**:
1. **Kondisi Mobil Baru Dinyalakan (ACC ON / Idle)**:
   - Menampilkan ringkasan kesehatan aki (*cranking voltage*) dan hitung mundur servis ganti Oli Mesin (5.000 km) & Filter Solar (10.000 km).
2. **Kondisi Berjalan / Melaju di Jalan Raya**:
   - Otomatis beralih ke *Trip Odometer*, jam tempuh, dan indikator elevasi tanjakan (MDPL).
3. **Kondisi Ganti Lagu / Flashdisk USB**:
   - Menampilkan info lagu dan album art selama 6 detik, lalu kembali ke mode mengemudi secara elegan.
   - *Opsi Pengemudi*: Tetap bisa menekan tombol pin [📌] jika ingin mengunci satu tampilan secara permanen.

### D. Fitur Kontrol Kenyamanan Mengemudi (Driver Utility)
1. **Tombol "Screen Off / Stealth Mode"**:
   - Satu sentuhan di bar atas, layar langsung mati total (layar hitam pekat 100%), namun musik, radio, dan panduan suara Google Maps tetap berbunyi di latar belakang.
   - Sangat berguna saat melewati jalanan tol gelap malam hari agar mata tidak cepat lelah.
   - Sentuh sembarang tempat di layar untuk menghidupkan kembali seketika.
2. **Master Volume Bar**:
   - Tombol volume fisik bawaan headunit Cina sering kali berupa tombol sentuh licin di tepi kiri yang susah ditekan saat jalanan keriting. Slider volume besar di bar bawah menyelesaikan masalah ini secara tuntas.

### E. Master Audio & DSP Acoustic Suite (Dual-Engine Audio)

1. **Dual-Engine Compatibility**:
   - **Primary Engine**: Memanfaatkan *Android AudioEffect API* (10-Band EQ, BassBoost, LoudnessEnhancer) secara langsung.
   - **Smart OEM Fallback**: Jika headunit menggunakan chip DSP proprietary tertutup (misal keluarga UIS7862 / PX6 tertentu), aplikasi menyediakan tombol bridge halus ke DSP internal tanpa menutup launcher.
2. **10-Band Graphic Equalizer Presisi**:
   - Frekuensi: `31Hz`, `62Hz`, `125Hz`, `250Hz`, `500Hz`, `1kHz`, `2kHz`, `4kHz`, `8kHz`, `16kHz`.
   - **Preset Akustik Khusus 2KD**:
     - *Innova Diesel Clarity* (vokal jernih di atas deru mesin diesel).
     - *Deep Bass Punch* (bass solid tanpa sember di pintu Innova).
3. **Kabin Sound Stage: Fader & Balance 3 Baris Innova**:
   - 🎯 **"Driver Focus"** (Fokus ke kursi sopir Innova depan kanan).
   - 👨‍👩‍👧‍👦 **"All Passenger"** (Rata ke 3 baris kursi keluarga).
   - 🔇 **"Front Only"** (Hanya baris depan saat anak tidur di belakang).
4. **Speed-Compensated Volume (SVC)**:
   - Volume audio otomatis naik halus (+1 s/d +3 level) saat mobil melaju kencang di tol via GPS, dan turun kembali saat pelan.
5. **Smart Navigation Ducking**:
   - Musik mengecil halus saat Google Maps/Waze memberikan petunjuk suara belok.

### F. Innova 2KD Fuse Box Quick Guide (Penyelamat Darurat Offline)
* **Kebutuhan Lapangan**: Saat lampu utama, colokan lighter, atau klakson Innova mati di malam hari, mencari buku manual sangat merepotkan.
* **Fitur**: Satu menu pop-up offline berisi:
  - **Diagram Sekring Ruang Mesin (Engine Bay)**: Letak sekring Horn, EFI, Headlamp, Starter.
  - **Diagram Sekring Bawah Setir (Cabin Fuse Box)**: Letak sekring CIG (lighter), Power Window, Audio, AC.
  - Sangat ringan (hanya gambar vektor + daftar tabel), nol kuota, tapi menjadi fitur penyelamat di saat darurat!

---

## 5. INTEGRASI JALUR KABEL FISIK HEADUNIT (FITUR MAKSIMAL TANPA DONGLE)

Memanfaatkan seluruh pin dan konektor kabel fisik bawaan soket headunit Innova 2009 secara optimal:

| Jalur Kabel Fisik | Sinyal Listrik | Fitur Cerdas yang Dihasilkan |
| :--- | :--- | :--- |
| **Kabel B+ (12V Aki) & ACC** | Tegangan Dinamis (ADC) | **Diesel Cranking Battery Sentinel**: Mendeteksi aki soak dari *voltage drop* saat starter (peringatan jika drop < 9.6V). |
| **Kabel Kontak ACC** | Sinyal Kunci Kontak ON/OFF | **Engine Hour Meter (Jam Terbang Mesin 2KD)**: Menghitung durasi riil mesin menyala saat macet untuk jadwal ganti oli yang lebih akurat. |
| **Kabel ILL (Illumination)** | +12V saat Lampu Senja ON | **Hardware-True Auto Dimmer**: Layar otomatis redup saat lampu senja dinyalakan (malam/hujan/terowongan) tanpa butuh internet. |
| **Port USB 1 / 2 (Laci Dashboard)**| USB Host OTG (Mass Storage) | **Offline Flashdisk Music Widget**: Memutar ribuan MP3 flashdisk langsung dari widget tengah tanpa buka app terpisah. |
| **Kabel Key 1 / Key 2 (SWC)** | Resistansi Tombol Setir | **Steering Switch Macro**: Tekan tahan tombol setir untuk aksi cepat (misal: Hold Mute = Matikan Layar). |
| **GPS + ACC Cut-Off Trigger** | Data Satelit + Event Shutdown | **Last Parking Memory**: Menyimpan koordinat & jam saat kontak dicabut untuk menghitung durasi parkir mobil. |

---

## 6. REKAYASA TEKNOLOGI: BAGAIMANA MEMASTIKAN SMOOTH & ANTI-LEMOT?

Untuk memastikan aplikasi ini berjalan mulus 60 FPS di Headunit dengan spesifikasi minim (RAM 1 GB / 2 GB, CPU Quad-Core Allwinner/Rockchip/MTK):

| Aspek Teknis | Standar Penerapan | Manfaat Nyata |
| :--- | :--- | :--- |
| **Arsitektur Kode** | 100% Native Android (Kotlin + Hardware Accelerated Canvas) | Nol beban Webview, konsumsi RAM stabil < 30 MB |
| **Lifecycle & Fast Boot** | Support Deep Sleep (`ACTION_SCREEN_ON` + `MCU_ACC_ON`) | Instan menyala < 1 detik saat kontak diputar, tanpa delay reboot |
| **Frame Rate** | 60 FPS V-Sync Lock | Transisi antar halaman mulus tanpa *dropped frames* |
| **GPS Handling** | Event-Driven Listener + Inertial Dead Reckoning | Tidak ada lag di lampu merah & anti-drop di kolong flyover |
| **Audio Processing** | Android Native OpenSL ES / AudioEffect API | Zero-Latency audio processing, hemat CPU < 2% |
| **Manajemen Memori** | Bitmap Recycling & Vector Drawables | Ukuran APK sangat kecil (< 8 MB), tidak ada memory leak |

---

## 7. ALUR NAVIGASI APLIKASI (USER FLOW)

1. **Kunci Kontak ON** ➔ Layar langsung menampilkan Cockpit Utama seketika (< 1 detik via Fastboot listener).
2. **Starter Mesin** ➔ Sensor membaca stabilitas voltase starter aki (notifikasi jika drop < 9.6V).
3. **Lampu Senja ON** ➔ Layar otomatis redup halus ke Night Amber via kabel fisik ILL.
4. **Mobil Melaju** ➔ Widget tengah otomatis beralih menampilkan Trip & Speedometer, volume audio menyesuaikan via SVC.
5. **Klik Salah Satu Shortcut (Maps/Musik/Radio)** ➔ Aplikasi terkait langsung terbuka.
6. **Klik [ ::: ALL APPS ::: ]** ➔ Terbuka laci aplikasi dengan grid 4x2 yang rapi dan mudah disentuh.
7. **Klik Ikon Audio / EQ di Bar Bawah** ➔ Membuka lembar DSP Audio Suite (10-Band EQ, Fader 3 Baris Innova, SVC, Bass Boost).
8. **Klik Ikon Darurat / Sekring** ➔ Membuka panduan cepat letak sekring Innova 2KD 2009.
9. **Tekan Tahan Slot Shortcut** ➔ Muncul modal dialog cepat: *"Pilih aplikasi untuk Slot 1"*.
10. **Klik Tombol Settings** ➔ Pengaturan tema (Amber/Cyan/Mono), batas kecepatan, kalibrasi odometer & jam servis 2KD.



