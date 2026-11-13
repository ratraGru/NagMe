package com.gru.nagme.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.animation.LinearInterpolator
import com.gru.nagme.domain.NagLevel
import kotlin.math.PI
import kotlin.math.sin

/**
 * Karakter preset "Blobby", digambar lewat Canvas (tanpa file gambar) + balon ucapan di atasnya.
 * Ekspresi mengikuti [level]. Bagian 5.6 / 13. Custom View, bukan Compose (Bagian 5.4).
 */
class CharacterView(context: Context) : View(context) {

    var level: NagLevel = NagLevel.CALM
        set(value) { field = value; invalidate() }
    /** Ekspresi senang (tugas selesai) — menggantikan ekspresi level. */
    var celebrate: Boolean = false
        set(value) { field = value; invalidate() }
    var line: String = ""
        set(value) { field = value; invalidate() }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val outline = Color(0xFF5A4A42.toInt(), stroke = true)
    private val fill = Color(0xFFFFFFFF.toInt())
    private val ink = Color(0xFF3B302B.toInt())
    private val white = Color(0xFFFFFFFF.toInt())
    private val sweat = Color(0xFFA9D8F0.toInt())
    private val blush = Color(0xFFF4A9A0.toInt())
    private val bubbleFill = Color(0xFFFFFBF2.toInt())
    private val bubbleText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3B302B.toInt()
        textSize = 15f * resources.displayMetrics.scaledDensity
    }

    private val body = RectF()
    private val bubble = RectF()
    private val path = Path()

    private var phase = 0f // 0..1 berulang, dipakai untuk napas & getar
    private val loop = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loop.start()
    }

    override fun onDetachedFromWindow() {
        loop.cancel()
        super.onDetachedFromWindow()
    }

    private fun bodyColor(): Int = if (celebrate) 0xFFBFE8D4.toInt() else when (level) {
        NagLevel.DORMANT, NagLevel.CALM -> 0xFFBFE8D4.toInt()   // mint
        NagLevel.WORRIED -> 0xFFF3E6A8.toInt()                  // krem kuning
        NagLevel.STRESSED -> 0xFFF8CDA6.toInt()                 // peach
        NagLevel.PANIC -> 0xFFF5B8A0.toInt()                    // peach tua
        NagLevel.CRITICAL -> 0xFFF2A9A0.toInt()                 // salmon (bukan merah menyala, P3)
        NagLevel.OVERDUE -> 0xFFC9C5D9.toInt()                  // lavender abu (ngambek)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val bubbleH = dp(BUBBLE_DP)
        drawBubble(canvas, w, bubbleH)

        val t = phase * 2f * PI.toFloat()
        val shaky = !celebrate && (level == NagLevel.PANIC || level == NagLevel.CRITICAL)
        val bob = sin(t) * dp(if (celebrate) 8f else if (shaky) 2f else 3f)
        val shake = if (shaky) sin(t * 6f) * dp(if (level == NagLevel.CRITICAL) 4f else 2f) else 0f

        val areaTop = bubbleH + dp(10f)
        val bodyH = (h - areaTop).coerceAtLeast(dp(60f)) * 0.92f
        val bodyW = bodyH * 1.05f
        val cx = w / 2f + shake
        val cy = areaTop + (h - areaTop) / 2f + bob
        body.set(cx - bodyW / 2f, cy - bodyH / 2f, cx + bodyW / 2f, cy + bodyH / 2f)

        fill.color = bodyColor()
        canvas.drawOval(body, fill)
        canvas.drawOval(body, outline)

        // Wajah: koordinat relatif terhadap pusat badan, satuan r.
        val r = bodyW / 2f
        drawFace(canvas, cx, cy, r)
    }

    private fun drawBubble(canvas: Canvas, w: Float, bubbleH: Float) {
        if (line.isBlank()) return
        val pad = dp(10f)
        bubble.set(dp(4f), dp(2f), w - dp(4f), bubbleH - dp(10f))
        canvas.drawRoundRect(bubble, dp(16f), dp(16f), bubbleFill)
        canvas.drawRoundRect(bubble, dp(16f), dp(16f), outline)
        // ekor balon
        path.reset()
        path.moveTo(w / 2f - dp(8f), bubble.bottom - 1f)
        path.lineTo(w / 2f, bubbleH)
        path.lineTo(w / 2f + dp(8f), bubble.bottom - 1f)
        canvas.drawPath(path, bubbleFill)
        canvas.drawLine(w / 2f - dp(8f), bubble.bottom, w / 2f, bubbleH, outline)
        canvas.drawLine(w / 2f + dp(8f), bubble.bottom, w / 2f, bubbleH, outline)

        val textW = (bubble.width() - 2 * pad).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder.obtain(line, 0, line.length, bubbleText, textW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(bubble.left + pad, bubble.top + (bubble.height() - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawFace(c: Canvas, cx: Float, cy: Float, r: Float) {
        val eyeDx = r * 0.36f
        val eyeY = cy - r * 0.10f
        val lw = outline.strokeWidth
        if (celebrate) {
            // mata tertutup senyum ^ ^, senyum lebar, pipi merona
            for (sx in floatArrayOf(-eyeDx, eyeDx)) {
                val ex = cx + sx
                c.drawArc(RectF(ex - r * 0.15f, eyeY - r * 0.10f, ex + r * 0.15f, eyeY + r * 0.20f), 180f, 180f, false, outlineNoFill())
            }
            smile(c, cx, cy + r * 0.22f, r * 0.34f, 1.6f)
            cheeks(c, cx, cy + r * 0.20f, r)
            outline.strokeWidth = lw
            return
        }
        when (level) {
            NagLevel.DORMANT, NagLevel.CALM -> {
                eye(c, cx - eyeDx, eyeY, r * 0.13f, r * 0.06f)
                eye(c, cx + eyeDx, eyeY, r * 0.13f, r * 0.06f)
                smile(c, cx, cy + r * 0.25f, r * 0.28f, 1f)
                cheeks(c, cx, cy + r * 0.20f, r)
            }
            NagLevel.WORRIED -> {
                eye(c, cx - eyeDx, eyeY, r * 0.14f, r * 0.07f)
                eye(c, cx + eyeDx, eyeY, r * 0.14f, r * 0.07f)
                brow(c, cx - eyeDx, eyeY - r * 0.28f, r * 0.2f, -0.35f)
                brow(c, cx + eyeDx, eyeY - r * 0.28f, r * 0.2f, 0.35f)
                smile(c, cx, cy + r * 0.38f, r * 0.2f, -0.6f)
                drop(c, cx + r * 0.72f, cy - r * 0.35f, r * 0.10f)
            }
            NagLevel.STRESSED -> {
                eye(c, cx - eyeDx, eyeY, r * 0.17f, r * 0.07f)
                eye(c, cx + eyeDx, eyeY, r * 0.17f, r * 0.07f)
                brow(c, cx - eyeDx, eyeY - r * 0.32f, r * 0.22f, -0.5f)
                brow(c, cx + eyeDx, eyeY - r * 0.32f, r * 0.22f, 0.5f)
                wavy(c, cx, cy + r * 0.40f, r * 0.34f)
                drop(c, cx + r * 0.75f, cy - r * 0.30f, r * 0.11f)
                drop(c, cx - r * 0.78f, cy - r * 0.05f, r * 0.09f)
            }
            NagLevel.PANIC -> {
                eye(c, cx - eyeDx, eyeY, r * 0.20f, r * 0.05f)
                eye(c, cx + eyeDx, eyeY, r * 0.20f, r * 0.05f)
                brow(c, cx - eyeDx, eyeY - r * 0.36f, r * 0.22f, -0.7f)
                brow(c, cx + eyeDx, eyeY - r * 0.36f, r * 0.22f, 0.7f)
                mouthO(c, cx, cy + r * 0.42f, r * 0.15f, r * 0.2f)
                drop(c, cx + r * 0.78f, cy - r * 0.30f, r * 0.12f)
                drop(c, cx - r * 0.80f, cy - r * 0.10f, r * 0.11f)
            }
            NagLevel.CRITICAL -> {
                eye(c, cx - eyeDx, eyeY, r * 0.23f, r * 0.04f)
                eye(c, cx + eyeDx, eyeY, r * 0.23f, r * 0.04f)
                brow(c, cx - eyeDx, eyeY - r * 0.40f, r * 0.24f, -0.8f)
                brow(c, cx + eyeDx, eyeY - r * 0.40f, r * 0.24f, 0.8f)
                mouthO(c, cx, cy + r * 0.44f, r * 0.24f, r * 0.30f)
                drop(c, cx + r * 0.80f, cy - r * 0.35f, r * 0.13f)
                drop(c, cx - r * 0.82f, cy - r * 0.15f, r * 0.12f)
                drop(c, cx + r * 0.62f, cy - r * 0.80f, r * 0.10f)
            }
            NagLevel.OVERDUE -> {
                // mata setengah tertutup, ngambek
                for (sx in floatArrayOf(-eyeDx, eyeDx)) {
                    val ex = cx + sx
                    c.drawArc(RectF(ex - r * 0.14f, eyeY - r * 0.14f, ex + r * 0.14f, eyeY + r * 0.14f), 0f, 180f, true, ink)
                    c.drawLine(ex - r * 0.18f, eyeY, ex + r * 0.18f, eyeY, outline)
                }
                brow(c, cx - eyeDx, eyeY - r * 0.22f, r * 0.2f, 0.45f)
                brow(c, cx + eyeDx, eyeY - r * 0.22f, r * 0.2f, -0.45f)
                smile(c, cx, cy + r * 0.40f, r * 0.2f, -0.5f)
            }
        }
        outline.strokeWidth = lw
    }

    /** Mata: bola putih + pupil. [pupil] kecil = lebih panik. */
    private fun eye(c: Canvas, x: Float, y: Float, rad: Float, pupil: Float) {
        c.drawCircle(x, y, rad, white)
        c.drawCircle(x, y, rad, outline)
        c.drawCircle(x, y, pupil.coerceAtLeast(dp(1.5f)), ink)
    }

    private fun brow(c: Canvas, x: Float, y: Float, halfLen: Float, tilt: Float) {
        c.drawLine(x - halfLen, y + halfLen * tilt, x + halfLen, y - halfLen * tilt, outline)
    }

    /** [curve] > 0 senyum, < 0 cemberut. */
    private fun smile(c: Canvas, x: Float, y: Float, halfW: Float, curve: Float) {
        path.reset()
        path.moveTo(x - halfW, y)
        path.quadTo(x, y + halfW * curve * 1.2f, x + halfW, y)
        c.drawPath(path, outlineNoFill())
    }

    private fun wavy(c: Canvas, x: Float, y: Float, halfW: Float) {
        path.reset()
        path.moveTo(x - halfW, y)
        val seg = halfW / 2f
        path.quadTo(x - halfW + seg / 2, y - seg * 0.6f, x - seg, y)
        path.quadTo(x - seg / 2 + seg / 2, y + seg * 0.6f, x, y)
        path.quadTo(x + seg / 2, y - seg * 0.6f, x + seg, y)
        path.quadTo(x + halfW - seg / 2, y + seg * 0.6f, x + halfW, y)
        c.drawPath(path, outlineNoFill())
    }

    private fun mouthO(c: Canvas, x: Float, y: Float, rx: Float, ry: Float) {
        val oval = RectF(x - rx, y - ry / 2, x + rx, y + ry)
        c.drawOval(oval, ink)
        c.drawOval(oval, outline)
    }

    private fun drop(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        path.moveTo(x, y - s * 1.6f)
        path.quadTo(x + s * 1.1f, y, x, y + s * 0.7f)
        path.quadTo(x - s * 1.1f, y, x, y - s * 1.6f)
        c.drawPath(path, sweat)
    }

    private fun cheeks(c: Canvas, cx: Float, y: Float, r: Float) {
        blush.alpha = 120
        c.drawCircle(cx - r * 0.55f, y, r * 0.09f, blush)
        c.drawCircle(cx + r * 0.55f, y, r * 0.09f, blush)
    }

    private val noFill = Color(0xFF5A4A42.toInt(), stroke = true)
    private fun outlineNoFill(): Paint = noFill

    @Suppress("FunctionName")
    private fun Color(color: Int, stroke: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        if (stroke) {
            style = Paint.Style.STROKE
            strokeWidth = 3f * resources.displayMetrics.density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    companion object {
        const val BUBBLE_DP = 78f
    }
}
