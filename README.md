# Timer Telur (Android)

Timer rebus telur 7 menit 30 detik. Saat waktu habis, alarm kokok ayam berbunyi
walau layar mati atau aplikasi sudah ditutup.

## Cara membuat APK
1. Install Android Studio (versi terbaru) dan JDK 17 (sudah termasuk di Android Studio).
2. File > Open, pilih folder `TimerTelur`. Tunggu Gradle sync selesai (butuh internet).
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. File APK ada di `app/build/outputs/apk/debug/app-debug.apk`. Kirim ke HP dan install
   (izinkan "install dari sumber tidak dikenal"), atau sambungkan HP dan tekan Run.

## Cara kerja saat layar mati
- Tombol Mulai mendaftarkan alarm sistem (`AlarmManager.setAlarmClock`) yang tetap tepat waktu
  di mode hemat daya (Doze).
- Saat alarm menyala, `AlarmService` (foreground service) memutar `res/raw/kukuruyuk.wav`
  berulang dengan jalur suara alarm, menahan wake lock, dan menggetarkan HP.
- Matikan lewat tombol di notifikasi atau di aplikasi. Alarm berhenti sendiri setelah 2 menit.

## Catatan
- Izinkan notifikasi saat diminta (Android 13 ke atas).
- Suara memakai volume alarm. Pastikan volume alarm tidak nol dan HP tidak dalam mode senyap total.
- Beberapa HP (Xiaomi, OPPO, Vivo, Samsung) agresif mematikan aplikasi. Bila alarm tidak bunyi,
  matikan optimasi baterai untuk Timer Telur dan izinkan "autostart".
- Timer tidak otomatis lanjut kalau HP dimatikan atau restart.
- Suara kokok disintesis lewat skrip (bukan rekaman ayam asli). Ganti dengan rekaman
  sendiri dengan menimpa `app/src/main/res/raw/kukuruyuk.wav` (nama file harus sama).
