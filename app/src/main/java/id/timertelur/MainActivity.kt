package id.timertelur

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var egg: EggView
    private lateinit var status: TextView
    private lateinit var mainBtn: Button
    private lateinit var resetBtn: Button
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 250) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun styled(b: Button, bg: String, fg: String) {
        b.setTextColor(Color.parseColor(fg))
        b.textSize = 18f
        b.isAllCaps = false
        b.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat(); setColor(Color.parseColor(bg))
        }
        b.minimumHeight = dp(56)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        egg = EggView(this)
        root.addView(egg, LinearLayout.LayoutParams(dp(260), dp(338)))

        status = TextView(this).apply {
            textSize = 17f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#12343B"))
        }
        root.addView(status, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16); bottomMargin = dp(16) })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        mainBtn = Button(this).also { styled(it, "#F2A900", "#12343B") }
        resetBtn = Button(this).apply { text = "Ulang" }.also { styled(it, "#00000000", "#12343B") }
        row.addView(mainBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(resetBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { leftMargin = dp(12) })
        root.addView(row, LinearLayout.LayoutParams(dp(300), LinearLayout.LayoutParams.WRAP_CONTENT))
        setContentView(root)

        mainBtn.setOnClickListener {
            when (TimerStore.state(this)) {
                "idle", "paused" -> start()
                "running" -> pause()
                "done" -> stopAlarm()
            }
            render()
        }
        resetBtn.setOnClickListener { reset(); render() }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun start() {
        val rem = if (TimerStore.state(this) == "paused") TimerStore.remaining(this) else TimerStore.TOTAL_MS
        val at = System.currentTimeMillis() + rem
        TimerStore.set(this, "running", at, rem)
        AlarmScheduler.schedule(this, at)
    }

    private fun pause() {
        val rem = maxOf(0L, TimerStore.endAt(this) - System.currentTimeMillis())
        AlarmScheduler.cancel(this)
        TimerStore.set(this, "paused", 0L, rem)
    }

    private fun reset() {
        AlarmScheduler.cancel(this)
        stopAlarm()
        TimerStore.reset(this)
    }

    private fun stopAlarm() {
        startService(Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_STOP))
        TimerStore.reset(this)
    }

    private fun fmt(ms: Long): String {
        val s = (ms + 999) / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun render() {
        val st = TimerStore.state(this)
        val rem = when (st) {
            "running" -> maxOf(0L, TimerStore.endAt(this) - System.currentTimeMillis())
            "paused" -> TimerStore.remaining(this)
            "done" -> 0L
            else -> TimerStore.TOTAL_MS
        }
        egg.progress = rem.toFloat() / TimerStore.TOTAL_MS
        egg.label = fmt(rem)
        val done = st == "done"
        root.setBackgroundColor(Color.parseColor(if (done) "#C8362B" else "#DCEFEA"))
        status.setTextColor(Color.parseColor(if (done) "#FFFFFF" else "#12343B"))
        status.text = when (st) {
            "running" -> "Telur sedang direbus. Layar boleh dimatikan."
            "paused" -> "Dijeda."
            "done" -> "Telur matang! Angkat sekarang."
            else -> "Siap merebus 7 menit 30 detik."
        }
        mainBtn.text = when (st) {
            "running" -> "Jeda"
            "paused" -> "Lanjut"
            "done" -> "Matikan alarm"
            else -> "Mulai"
        }
        resetBtn.visibility = if (done) android.view.View.GONE else android.view.View.VISIBLE
    }

    override fun onResume() { super.onResume(); handler.post(ticker) }
    override fun onPause() { handler.removeCallbacks(ticker); super.onPause() }
}
