package com.gru.nagme.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils
import com.gru.nagme.domain.NagLevel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * Karakter NagMe, digambar lewat Canvas (tanpa file gambar) + balon ucapan di atasnya.
 * Tiga karakter asli buatan sendiri: Blobby (tunas daun), Kucing (telinga + kumis), Kelinci (telinga panjang).
 * Badan bergradasi dengan kilau, mata oval besar; ekspresi mengikuti [level] (Bagian 5.6 / 13).
 * Custom View, bukan Compose (Bagian 5.4). Semua objek gambar dipakai ulang agar ringan.
 */
class CharacterView(context: Context) : View(context) {

    var level: NagLevel = NagLevel.CALM
        set(value) { field = value; invalidate() }
    /** Ekspresi senang (tugas selesai) — menggantikan ekspresi level. */
    var celebrate: Boolean = false
        set(value) { field = value; invalidate() }
    var line: String = ""
        set(value) { field = value; invalidate() }
    /** Tanpa balon ucapan (untuk thumbnail dan ilustrasi kecil). */
    var compact: Boolean = false
        set(value) { field = value; invalidate() }
    /** False = diam (tanpa animasi napas/getar), hemat tenaga untuk thumbnail. */
    var animated: Boolean = true

    /** 0 = Blobby, 1 = Kucing, 2 = Kelinci. */
    var style: Int = 0
        set(value) { field = value; invalidate() }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // ---- cat & palet ----
    private val ink = 0xFF2D2140.toInt()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val inkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
    private val inkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 3.2f * density
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30000000 }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF }
    private val innerEarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99FF9EB5.toInt() }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FF6F91 }
    private val sweatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9EDCFF.toInt() }
    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF34B58C.toInt() }
    private val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF8FA3.toInt() }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD86B.toInt() }
    private val whiskerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 1.6f * density
    }
    private val bubbleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val bubbleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26000000
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    private val bubbleText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 15f * resources.displayMetrics.scaledDensity
    }

    private val body = RectF()
    private val bubble = RectF()
    private val tmp = RectF()
    private val path = Path()

    private var shaderKey = Int.MIN_VALUE

    private var phase = 0f // 0..1 berulang, dipakai untuk napas, getar, dan kedip
    private val loop = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animated) loop.start()
    }

    override fun onDetachedFromWindow() {
        loop.cancel()
        super.onDetachedFromWindow()
    }

    private fun baseColor(): Int = when (style) {
        1 -> 0xFFFFA77A.toInt()   // Kucing: persik
        2 -> 0xFFD9A7FF.toInt()   // Kelinci: lilac
        else -> 0xFF74DDBE.toInt() // Blobby: mint
    }

    /** Warna badan bergeser hangat saat makin panik, dan keabu-abuan saat ngambek (bukan merah menyala, P3). */
    private fun bodyColor(): Int {
        val base = baseColor()
        if (celebrate) return base
        return when (level) {
            NagLevel.DORMANT, NagLevel.CALM, NagLevel.WORRIED -> base
            NagLevel.STRESSED -> ColorUtils.blendARGB(base, ROSE, 0.15f)
            NagLevel.PANIC -> ColorUtils.blendARGB(base, ROSE, 0.30f)
            NagLevel.CRITICAL -> ColorUtils.blendARGB(base, ROSE, 0.45f)
            NagLevel.OVERDUE -> ColorUtils.blendARGB(base, GRAY, 0.60f)
        }
    }

    private fun updateShader(top: Float, bottom: Float) {
        val c = bodyColor()
        val key = (c * 31 + top.toInt()) * 31 + bottom.toInt()
        if (key == shaderKey) return
        shaderKey = key
        fillPaint.shader = LinearGradient(
            0f, top, 0f, bottom,
            ColorUtils.blendARGB(c, Color.WHITE, 0.30f),
            ColorUtils.blendARGB(c, Color.BLACK, 0.12f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val bubbleH = if (compact) 0f else dp(BUBBLE_DP)
        if (!compact) drawBubble(canvas, w, bubbleH)

        val t = phase * 2f * PI.toFloat()
        val shaky = !celebrate && (level == NagLevel.PANIC || level == NagLevel.CRITICAL)
        val bob = sin(t) * dp(if (celebrate) 8f else if (shaky) 2f else 3f)
        val shake = if (shaky) sin(t * 6f) * dp(if (level == NagLevel.CRITICAL) 4f else 2f) else 0f

        val areaTop = bubbleH + dp(10f)
        val availH = (h - areaTop).coerceAtLeast(dp(60f))
        // Telinga/tunas butuh ruang di atas badan, jadi badannya sedikit lebih kecil.
        var bodyH = availH * when (style) { 1 -> 0.72f; 2 -> 0.66f; else -> 0.80f }
        var bodyW = bodyH * 1.08f
        bodyW = min(bodyW, w - dp(8f))
        bodyH = min(bodyH, bodyW / 1.08f)
        val cx0 = w / 2f
        val bottom = h - dp(14f)
        body.set(cx0 - bodyW / 2f, bottom - bodyH, cx0 + bodyW / 2f, bottom)

        // Bayangan di tanah (diam), mengecil sedikit saat karakter naik.
        val squash = 1f - bob / (bodyH * 3f)
        tmp.set(cx0 - bodyW * 0.36f * squash, h - dp(12f) - dp(4f), cx0 + bodyW * 0.36f * squash, h - dp(12f) + dp(4f))
        canvas.drawOval(tmp, shadowPaint)

        canvas.save()
        canvas.translate(shake, bob - dp(0f))
        updateShader(body.top - bodyH * 0.3f, body.bottom)
        drawTopParts(canvas, cx0, bodyW, bodyH)
        val rr = min(bodyW, bodyH) * 0.46f
        canvas.drawRoundRect(body, rr, rr, fillPaint)
        // kilau
        tmp.set(body.left + bodyW * 0.13f, body.top + bodyH * 0.08f, body.left + bodyW * 0.43f, body.top + bodyH * 0.22f)
        canvas.drawOval(tmp, glossPaint)
        canvas.drawCircle(body.left + bodyW * 0.50f, body.top + bodyH * 0.12f, bodyW * 0.022f, glossPaint)

        val r = bodyW / 2f
        val cy = body.top + bodyH * 0.52f
        drawFace(canvas, cx0, cy, r, t)
        if (celebrate) drawSparkles(canvas, cx0, cy, r, t)
        canvas.restore()
    }

    /** Telinga / tunas digambar sebelum badan, jadi pangkalnya tertutup badan. */
    private fun drawTopParts(c: Canvas, cx: Float, bodyW: Float, bodyH: Float) {
        val top = body.top
        for (dir in floatArrayOf(-1f, 1f)) {
            when (style) {
                1 -> { // kucing: telinga segitiga membulat + bagian dalam pink
                    path.reset()
                    path.moveTo(cx + dir * bodyW * 0.46f, top + bodyH * 0.32f)
                    path.quadTo(cx + dir * bodyW * 0.52f, top - bodyH * 0.24f, cx + dir * bodyW * 0.16f, top + bodyH * 0.05f)
                    path.close()
                    c.drawPath(path, fillPaint)
                    path.reset()
                    path.moveTo(cx + dir * bodyW * 0.41f, top + bodyH * 0.25f)
                    path.quadTo(cx + dir * bodyW * 0.45f, top - bodyH * 0.10f, cx + dir * bodyW * 0.24f, top + bodyH * 0.06f)
                    path.close()
                    c.drawPath(path, innerEarPaint)
                }
                2 -> { // kelinci: telinga panjang, sedikit miring
                    val ex = cx + dir * bodyW * 0.24f
                    c.save()
                    c.rotate(dir * 9f, ex, top + bodyH * 0.12f)
                    tmp.set(ex - bodyW * 0.12f, top - bodyH * 0.46f, ex + bodyW * 0.12f, top + bodyH * 0.18f)
                    c.drawRoundRect(tmp, bodyW * 0.12f, bodyW * 0.12f, fillPaint)
                    tmp.set(ex - bodyW * 0.06f, top - bodyH * 0.36f, ex + bodyW * 0.06f, top + bodyH * 0.10f)
                    c.drawRoundRect(tmp, bodyW * 0.06f, bodyW * 0.06f, innerEarPaint)
                    c.restore()
                }
                else -> { // blobby: tunas dua daun
                    path.reset()
                    path.moveTo(cx, top + bodyH * 0.05f)
                    path.quadTo(cx + dir * bodyW * 0.03f, top - bodyH * 0.14f, cx + dir * bodyW * 0.20f, top - bodyH * 0.17f)
                    path.quadTo(cx + dir * bodyW * 0.16f, top - bodyH * 0.02f, cx, top + bodyH * 0.05f)
                    path.close()
                    c.drawPath(path, leafPaint)
                }
            }
        }
    }

    private fun drawBubble(canvas: Canvas, w: Float, bubbleH: Float) {
        if (line.isBlank()) return
        val pad = dp(10f)
        bubble.set(dp(4f), dp(2f), w - dp(4f), bubbleH - dp(10f))
        canvas.drawRoundRect(bubble, dp(18f), dp(18f), bubbleFill)
        canvas.drawRoundRect(bubble, dp(18f), dp(18f), bubbleStroke)
        // ekor balon
        path.reset()
        path.moveTo(w / 2f - dp(8f), bubble.bottom - dp(1.5f))
        path.lineTo(w / 2f, bubbleH)
        path.lineTo(w / 2f + dp(8f), bubble.bottom - dp(1.5f))
        path.close()
        canvas.drawPath(path, bubbleFill)

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

    // ---------------------------------------------------------------- wajah

    private fun drawFace(c: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        val eyeDx = r * 0.38f
        val eyeY = cy - r * 0.10f
        val mouthY = cy + r * 0.36f
        val blink = blinkFactor()

        if (celebrate) {
            for (sx in floatArrayOf(-eyeDx, eyeDx)) {
                val ex = cx + sx
                tmp.set(ex - r * 0.15f, eyeY - r * 0.10f, ex + r * 0.15f, eyeY + r * 0.16f)
                c.drawArc(tmp, 200f, 140f, false, inkStroke)
            }
            openSmile(c, cx, mouthY - r * 0.04f, r * 0.30f)
            cheeks(c, cx, mouthY - r * 0.04f, r, 1.3f)
            muzzle(c, cx, cy, r)
            return
        }
        when (level) {
            NagLevel.DORMANT, NagLevel.CALM -> {
                eye(c, cx - eyeDx, eyeY, r * 0.17f, 0.62f, 0f, 0f, blink)
                eye(c, cx + eyeDx, eyeY, r * 0.17f, 0.62f, 0f, 0f, blink)
                smile(c, cx, mouthY, r * 0.20f, 1f)
                cheeks(c, cx, mouthY, r, 1f)
            }
            NagLevel.WORRIED -> {
                eye(c, cx - eyeDx, eyeY, r * 0.18f, 0.55f, 0.15f, -0.2f, blink)
                eye(c, cx + eyeDx, eyeY, r * 0.18f, 0.55f, -0.15f, -0.2f, blink)
                brow(c, cx - eyeDx, eyeY - r * 0.34f, r * 0.17f, 0.45f, -1f)
                brow(c, cx + eyeDx, eyeY - r * 0.34f, r * 0.17f, 0.45f, 1f)
                smile(c, cx, mouthY + r * 0.04f, r * 0.15f, -0.7f)
                drop(c, cx + r * 0.76f, cy - r * 0.35f, r * 0.10f)
            }
            NagLevel.STRESSED -> {
                eye(c, cx - eyeDx, eyeY, r * 0.20f, 0.46f, 0f, -0.1f, blink)
                eye(c, cx + eyeDx, eyeY, r * 0.20f, 0.46f, 0f, -0.1f, blink)
                brow(c, cx - eyeDx, eyeY - r * 0.38f, r * 0.19f, 0.6f, -1f)
                brow(c, cx + eyeDx, eyeY - r * 0.38f, r * 0.19f, 0.6f, 1f)
                wavy(c, cx, mouthY + r * 0.04f, r * 0.26f)
                drop(c, cx + r * 0.78f, cy - r * 0.30f, r * 0.11f)
                drop(c, cx - r * 0.80f, cy - r * 0.05f, r * 0.09f)
            }
            NagLevel.PANIC -> {
                eye(c, cx - eyeDx, eyeY, r * 0.23f, 0.34f, 0f, 0f, 1f)
                eye(c, cx + eyeDx, eyeY, r * 0.23f, 0.34f, 0f, 0f, 1f)
                brow(c, cx - eyeDx, eyeY - r * 0.44f, r * 0.20f, 0.8f, -1f)
                brow(c, cx + eyeDx, eyeY - r * 0.44f, r * 0.20f, 0.8f, 1f)
                mouthO(c, cx, mouthY + r * 0.06f, r * 0.13f, r * 0.18f)
                drop(c, cx + r * 0.80f, cy - r * 0.30f, r * 0.12f)
                drop(c, cx - r * 0.82f, cy - r * 0.10f, r * 0.11f)
            }
            NagLevel.CRITICAL -> {
                val jitter = sin(t * 7f) * 0.2f
                eye(c, cx - eyeDx, eyeY, r * 0.26f, 0.30f, jitter, 0f, 1f)
                eye(c, cx + eyeDx, eyeY, r * 0.26f, 0.30f, jitter, 0f, 1f)
                brow(c, cx - eyeDx, eyeY - r * 0.50f, r * 0.21f, 0.9f, -1f)
                brow(c, cx + eyeDx, eyeY - r * 0.50f, r * 0.21f, 0.9f, 1f)
                mouthO(c, cx, mouthY + r * 0.08f, r * 0.19f, r * 0.26f)
                drop(c, cx + r * 0.82f, cy - r * 0.35f, r * 0.13f)
                drop(c, cx - r * 0.84f, cy - r * 0.15f, r * 0.12f)
                drop(c, cx + r * 0.62f, cy - r * 0.84f, r * 0.10f)
            }
            NagLevel.OVERDUE -> {
                // mata setengah tertutup, ngambek
                for (sx in floatArrayOf(-eyeDx, eyeDx)) {
                    eye(c, cx + sx, eyeY, r * 0.17f, 0.60f, 0f, 0.25f, 0.5f)
                }
                brow(c, cx - eyeDx, eyeY - r * 0.26f, r * 0.19f, -0.45f, -1f)
                brow(c, cx + eyeDx, eyeY - r * 0.26f, r * 0.19f, -0.45f, 1f)
                smile(c, cx, mouthY + r * 0.06f, r * 0.16f, -0.5f)
            }
        }
        muzzle(c, cx, cy, r)
    }

    /** Kedip singkat sekali per siklus; 1 = terbuka penuh. */
    private fun blinkFactor(): Float {
        val p = phase
        return if (animated && p > 0.92f) abs(p - 0.96f) / 0.04f * 0.9f + 0.1f else 1f
    }

    /** Mata oval: putih + pupil + kilatan. [open] < 1 = setengah tertutup / berkedip. */
    private fun eye(c: Canvas, x: Float, y: Float, rx: Float, pupilK: Float, lookX: Float, lookY: Float, open: Float) {
        val ry = rx * 1.3f * open
        tmp.set(x - rx, y - ry, x + rx, y + ry)
        c.drawOval(tmp, whitePaint)
        val pr = rx * pupilK
        val px = x + lookX * (rx - pr)
        val py = y + lookY * (ry - pr).coerceAtLeast(0f) + if (open < 1f) ry * 0.25f else 0f
        c.save()
        if (open < 1f) c.clipRect(x - rx, y - ry, x + rx, y + ry) // pupil tidak keluar dari mata
        c.drawCircle(px, py, pr, inkFill)
        c.drawCircle(px - pr * 0.32f, py - pr * 0.36f, pr * 0.28f, whitePaint)
        c.restore()
        if (open < 0.7f) {
            // kelopak: garis tepi atas mata
            c.drawLine(x - rx * 1.05f, y - ry * 0.05f, x + rx * 1.05f, y - ry * 0.05f, inkStroke)
        }
    }

    /**
     * Alis tebal membulat. [inner] > 0 = ujung dalam naik (khawatir), < 0 = ujung dalam turun (kesal).
     * [dir] = -1 mata kiri, +1 mata kanan.
     */
    private fun brow(c: Canvas, x: Float, y: Float, halfLen: Float, inner: Float, dir: Float) {
        val outerX = x + dir * halfLen
        val innerX = x - dir * halfLen
        c.drawLine(outerX, y + inner * halfLen, innerX, y - inner * halfLen, inkStroke)
    }

    /** [curve] > 0 senyum, < 0 cemberut. */
    private fun smile(c: Canvas, x: Float, y: Float, halfW: Float, curve: Float) {
        path.reset()
        path.moveTo(x - halfW, y)
        path.quadTo(x, y + halfW * curve * 1.3f, x + halfW, y)
        c.drawPath(path, inkStroke)
    }

    private fun openSmile(c: Canvas, x: Float, y: Float, halfW: Float) {
        path.reset()
        path.moveTo(x - halfW, y)
        path.quadTo(x, y + halfW * 1.9f, x + halfW, y)
        path.close()
        c.drawPath(path, inkFill)
        c.save()
        c.clipPath(path)
        tmp.set(x - halfW * 0.5f, y + halfW * 0.55f, x + halfW * 0.5f, y + halfW * 1.3f)
        c.drawOval(tmp, nosePaint)
        c.restore()
    }

    private fun wavy(c: Canvas, x: Float, y: Float, halfW: Float) {
        path.reset()
        val seg = halfW / 2f
        path.moveTo(x - halfW, y)
        path.quadTo(x - halfW + seg / 2f, y - seg * 0.7f, x - seg, y)
        path.quadTo(x - seg / 2f, y + seg * 0.7f, x, y)
        path.quadTo(x + seg / 2f, y - seg * 0.7f, x + seg, y)
        path.quadTo(x + halfW - seg / 2f, y + seg * 0.7f, x + halfW, y)
        c.drawPath(path, inkStroke)
    }

    private fun mouthO(c: Canvas, x: Float, y: Float, rx: Float, ry: Float) {
        tmp.set(x - rx, y - ry / 2f, x + rx, y + ry)
        c.drawOval(tmp, inkFill)
    }

    private fun drop(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        path.moveTo(x, y - s * 1.7f)
        path.quadTo(x + s * 1.2f, y, x, y + s * 0.8f)
        path.quadTo(x - s * 1.2f, y, x, y - s * 1.7f)
        c.drawPath(path, sweatPaint)
        c.drawCircle(x - s * 0.3f, y - s * 0.1f, s * 0.22f, whitePaint)
    }

    private fun cheeks(c: Canvas, cx: Float, y: Float, r: Float, k: Float) {
        tmp.set(cx - r * 0.72f - r * 0.12f * k, y - r * 0.07f * k, cx - r * 0.72f + r * 0.12f * k, y + r * 0.07f * k)
        c.drawOval(tmp, blushPaint)
        tmp.set(cx + r * 0.72f - r * 0.12f * k, y - r * 0.07f * k, cx + r * 0.72f + r * 0.12f * k, y + r * 0.07f * k)
        c.drawOval(tmp, blushPaint)
    }

    /** Detail khas karakter: hidung & kumis (Kucing), hidung (Kelinci). */
    private fun muzzle(c: Canvas, cx: Float, cy: Float, r: Float) {
        when (style) {
            1 -> {
                path.reset()
                path.moveTo(cx - r * 0.06f, cy + r * 0.12f)
                path.lineTo(cx + r * 0.06f, cy + r * 0.12f)
                path.lineTo(cx, cy + r * 0.20f)
                path.close()
                c.drawPath(path, nosePaint)
                for (dir in floatArrayOf(-1f, 1f)) {
                    for (k in 0..2) {
                        val yy = cy + r * (0.14f + k * 0.07f)
                        c.drawLine(cx + dir * r * 0.62f, yy, cx + dir * r * 0.92f, yy + (k - 1) * r * 0.05f, whiskerPaint)
                    }
                }
            }
            2 -> {
                tmp.set(cx - r * 0.06f, cy + r * 0.12f, cx + r * 0.06f, cy + r * 0.20f)
                c.drawOval(tmp, nosePaint)
            }
        }
    }

    private fun drawSparkles(c: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        val pulse = 0.75f + 0.25f * sin(t)
        star(c, cx - r * 0.98f, cy - r * 0.85f, r * 0.14f * pulse)
        star(c, cx + r * 0.98f, cy - r * 0.60f, r * 0.11f * (1.5f - pulse))
        star(c, cx + r * 0.70f, cy - r * 1.10f, r * 0.09f * pulse)
    }

    private fun star(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        path.moveTo(x, y - s)
        path.quadTo(x, y, x + s, y)
        path.quadTo(x, y, x, y + s)
        path.quadTo(x, y, x - s, y)
        path.quadTo(x, y, x, y - s)
        c.drawPath(path, starPaint)
    }

    companion object {
        const val BUBBLE_DP = 78f
        private val ROSE = 0xFFFF7B93.toInt()
        private val GRAY = 0xFF9A97B5.toInt()
    }
}
