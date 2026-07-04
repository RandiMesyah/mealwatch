# MealWatch

Aplikasi Android untuk membantu memantau anak saat makan: mendeteksi apakah anak
ada di depan kamera dan apakah dia terlihat mengunyah, lalu memberi peringatan
suara (Text-to-Speech Bahasa Indonesia) kalau tidak terdeteksi mengunyah dalam
waktu tertentu atau kalau anak keluar dari jangkauan kamera.

## Cara dapat file APK TANPA install Android Studio (pakai GitHub Actions)
Kalau kamu tidak punya Android Studio, kamu tetap bisa dapat file `.apk` siap
install, karena project ini sudah dilengkapi workflow otomatis
(`.github/workflows/build.yml`) yang akan di-build oleh server GitHub, bukan
di komputer/HP kamu.

Langkah-langkah:
1. Buat akun GitHub gratis di https://github.com kalau belum punya.
2. Buat repository baru (bisa **Private**), misalnya nama `mealwatch`.
3. Upload seluruh isi folder `MealWatch` ini ke repo tersebut. Cara paling
   mudah: di halaman repo GitHub, klik "uploading an existing file", lalu
   drag & drop semua file/folder (atau extract dulu zip-nya, lalu upload).
4. Setelah upload selesai (otomatis masuk ke branch `main`), buka tab
   **Actions** di repo tersebut. Workflow "Build APK" akan otomatis jalan.
5. Tunggu sampai selesai (tanda centang hijau, biasanya 3–5 menit).
6. Klik hasil run yang selesai tadi, scroll ke bagian **Artifacts**, lalu
   download `MealWatch-debug-apk` — isinya file `app-debug.apk`.
7. Pindahkan file `.apk` itu ke HP (lewat WhatsApp ke diri sendiri, Google
   Drive, atau kabel USB), lalu install seperti biasa (perlu mengizinkan
   "Install dari sumber tidak dikenal" di HP).

Kalau upload lewat browser terasa merepotkan karena banyak folder, kamu juga
bisa pakai git dari terminal:
```
cd MealWatch
git init
git add .
git commit -m "Initial commit MealWatch"
git branch -M main
git remote add origin https://github.com/USERNAME/mealwatch.git
git push -u origin main
```

## Cara membuka project
1. Install **Android Studio** (versi terbaru).
2. `File > Open` lalu pilih folder `MealWatch` ini.
3. Tunggu Gradle sync selesai (butuh koneksi internet untuk download dependency
   pertama kali: CameraX & ML Kit Face Mesh).
4. Jalankan (`Run`) ke **HP Android fisik** — bukan emulator, karena emulator
   biasanya tidak punya kamera depan yang berfungsi baik untuk ML Kit.
5. Saat pertama dibuka, izinkan permission kamera.

## Cara kerja singkat
- `FaceMeshAnalyzer.kt` — setiap frame kamera dianalisis dengan ML Kit Face Mesh
  (468 titik wajah), lalu dihitung rasio jarak bibir atas-bawah dibagi lebar
  wajah (`mouthOpenRatio`). Ini dipakai supaya deteksi tidak terpengaruh jarak
  anak ke kamera.
- `ChewMonitor.kt` — dari rasio itu, disimpulkan kapan mulut "terbuka" lalu
  "tertutup" berulang kali dalam jendela waktu tertentu → dianggap mengunyah.
  Kalau lama tidak ada osilasi buka-tutup, atau wajah hilang dari kamera, akan
  memicu status peringatan.
- `ReminderManager.kt` — mengubah teks jadi suara (TextToSpeech), dengan
  beberapa variasi kalimat dan jeda (cooldown) 8 detik agar tidak bicara terus
  menerus / spam.
- `MainActivity.kt` — menyambungkan kamera depan (CameraX) ke analyzer, lalu
  ke monitor dan reminder, sambil menampilkan status di layar.

## Bagian yang perlu di-tuning langsung di HP anak (PENTING)
Deteksi wajah/mulut sangat tergantung kondisi kamera, jarak, dan pencahayaan.
Nilai default di `ChewMonitor.kt` adalah titik awal, bukan hasil final:

| Parameter | Default | Fungsi |
|---|---|---|
| `mouthOpenThreshold` | 0.18 | Ambang rasio dianggap "mulut terbuka". Kalau sering salah deteksi mengunyah padahal diam → naikkan. Kalau sulit terdeteksi mengunyah → turunkan. |
| `chewTransitionsThreshold` | 3 | Berapa kali buka-tutup dalam window agar dianggap "mengunyah" (bukan cuma bicara sekali). |
| `windowMs` | 6000 | Rentang waktu (ms) untuk menghitung buka-tutup tadi. |
| `idleWarningMs` | 20000 | Berapa lama tanpa gerakan mengunyah sebelum suara peringatan "ayo dikunyah" berbunyi. |
| `faceLostWarningMs` | 6000 | Berapa lama wajah tidak terdeteksi sebelum peringatan "duduk di tempat makan". |

Cara tuning paling praktis: jalankan app, lihat angka "Kunyahan terdeteksi" di
pojok kiri bawah sambil anak makan sungguhan, lalu sesuaikan angka di atas
sampai terasa akurat.

## Catatan & batasan penting
- Ini alat bantu, **bukan pengganti pengawasan langsung orang tua** — terutama
  untuk anak balita yang berisiko tersedak; aplikasi tidak bisa mendeteksi
  keadaan darurat seperti tersedak.
- Deteksi berbasis penglihatan mulut bisa salah kalau anak bicara sambil makan,
  memakai masker/menutup mulut, atau pencahayaan sangat gelap.
- HP sebaiknya diletakkan di dudukan/tripod stabil menghadap anak, bukan
  dipegang tangan, agar deteksi wajah stabil.
- Suara TTS memakai suara bawaan Android (bahasa Indonesia) — kalau HP belum
  punya paket bahasa Indonesia untuk TTS, buka
  `Setelan > Aksesibilitas/Bahasa > Text-to-Speech` lalu download data suara
  Bahasa Indonesia.

## Pengembangan lanjutan (opsional)
- Ganti suara robot TTS dengan rekaman suara ibu sendiri (`MediaPlayer` +
  file .mp3 di `res/raw`) — bisa saya buatkan kalau mau.
- Tambah logging riwayat makan (durasi makan, jumlah kunyahan) ke file/lokal
  database agar bisa dilihat orang tua nanti.
- Tambah deteksi durasi makan total dan estimasi porsi selesai.
