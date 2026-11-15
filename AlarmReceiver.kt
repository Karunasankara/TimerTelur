package id.timertelur

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerStore.set(context, "done", 0L, 0L)
        context.startForegroundService(Intent(context, AlarmService::class.java))
    }
}
