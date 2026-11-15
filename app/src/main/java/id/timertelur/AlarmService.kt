package id.timertelur

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator

/** Memutar kokok ayam berulang sebagai foreground service, jadi tetap bunyi walau layar mati. */
class AlarmService : Service() {

    companion object {
        const val ACTION_STOP = "id.timertelur.STOP"
        private const val CHANNEL_ID = "alarm_telur"
        private const val NOTIF_ID = 7
        private const val AUTO_STOP_MS = 2 * 60 * 1000L
    }

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopAlarm() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }
        showNotification()
        acquireWakeLock()
        startSound()
        startVibration()
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AUTO_STOP_MS)
        return START_NOT_STICKY
    }

    private fun showNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Alarm telur", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // suara diputar sendiri lewat MediaPlayer
                enableVibration(false)
            }
        )
        val open = PendingIntent.getActivity(
            this, 3, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 4, Intent(this, AlarmService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        @Suppress("DEPRECATION")
        val n = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Telur matang!")
            .setContentText("Angkat telurnya sekarang.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .addAction(android.R.drawable.ic_media_pause, "Matikan alarm", stop)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "timertelur:alarm").apply {
            acquire(AUTO_STOP_MS + 10_000L)
        }
    }

    private fun startSound() {
        player?.release()
        val afd = resources.openRawResourceFd(R.raw.kukuruyuk)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = true
            prepare()
            start()
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 400), 0))
    }

    @Suppress("DEPRECATION")
    private fun stopAlarm() {
        handler.removeCallbacks(autoStop)
        player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        player = null
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        TimerStore.reset(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        player?.release()
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
