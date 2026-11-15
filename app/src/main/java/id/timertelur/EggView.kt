package id.timertelur

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Typeface
import android.view.View

/** Telur yang garis kuningnya memendek sesuai sisa waktu. */
class EggView(context: Context) : View(context) {
    var progress = 1f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var label = "7:30"
        set(v) { field = v; invalidate() }

    private val shell = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF4DF") }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = Color.parseColor("#2212343B")
    }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = Color.parseColor("#F2A900")
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#12343B"); textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val base = Path().apply {
        moveTo(100f, 10f)
        cubicTo(150f, 10f, 185f, 100f, 185f, 155f)
        cubicTo(185f, 210f, 148f, 250f, 100f, 250f)
        cubicTo(52f, 250f, 15f, 210f, 15f, 155f)
        cubicTo(15f, 100f, 50f, 10f, 100f, 10f)
        close()
    }

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width / 200f, height / 260f)
        val m = Matrix().apply { setScale(s, s); postTranslate((width - 200f * s) / 2f, (height - 260f * s) / 2f) }
        val p = Path(base).also { it.transform(m) }
        track.strokeWidth = 9f * s
        bar.strokeWidth = 9f * s
        canvas.drawPath(p, shell)
        canvas.drawPath(p, track)
        val pm = PathMeasure(p, false)
        val seg = Path()
        if (progress > 0f && pm.getSegment(0f, pm.length * progress, seg, true)) canvas.drawPath(seg, bar)
        text.textSize = 52f * s
        canvas.drawText(label, width / 2f, height / 2f + 18f * s, text)
    }
}
