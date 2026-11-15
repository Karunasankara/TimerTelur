package id.timertelur

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Menyimpan status timer supaya tetap benar walau aplikasi ditutup atau layar mati. */
object TimerStore {
    const val TOTAL_MS = 450_000L // 7 menit 30 detik

    private fun prefs(c: Context) = c.getSharedPreferences("timer", Context.MODE_PRIVATE)

    fun state(c: Context): String = prefs(c).getString("state", "idle") ?: "idle"
    fun endAt(c: Context): Long = prefs(c).getLong("endAt", 0L)
    fun remaining(c: Context): Long = prefs(c).getLong("remaining", TOTAL_MS)

    fun set(c: Context, state: String, endAt: Long, remaining: Long) {
        prefs(c).edit()
            .putString("state", state)
            .putLong("endAt", endAt)
            .putLong("remaining", remaining)
            .apply()
    }

    fun reset(c: Context) = set(c, "idle", 0L, TOTAL_MS)
}

/** Alarm sistem. setAlarmClock tetap berbunyi tepat waktu saat layar mati dan mode Doze. */
object AlarmScheduler {
    private fun operation(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 1, Intent(c, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Mengembalikan false bila alarm tidak boleh/gagal dijadwalkan. */
    fun schedule(c: Context, triggerAtMs: Long): Boolean {
        val am = c.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) return false
        return try {
            val show = PendingIntent.getActivity(
                c, 2, Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMs, show), operation(c))
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancel(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(operation(c))
    }
}
