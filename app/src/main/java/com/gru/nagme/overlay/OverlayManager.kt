package com.gru.nagme.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import com.gru.nagme.domain.NagLevel

/**
 * Menampilkan / menyembunyikan karakter di atas aplikasi lain lewat WindowManager.
 * Harus dipanggil dari main thread. Bagian 5.4 + 14.
 *
 * Animasi digerakkan dengan mengubah posisi window itu sendiri (bukan translasi di dalam view),
 * supaya karakter bisa masuk dari luar layar tanpa terpotong batas window.
 */
class OverlayManager(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val density = appContext.resources.displayMetrics.density

    private var view: CharacterView? = null
    private var params: WindowManager.LayoutParams? = null
    private var animator: ValueAnimator? = null
    private var hideRunnable: Runnable? = null
    private var onHidden: (() -> Unit)? = null
    private var leaving = false

    fun isShowing(): Boolean = view != null

    /** Tampilkan karakter. [onHidden] dipanggil setelah view benar-benar dilepas. */
    fun show(level: NagLevel, line: String, sizeDp: Int = DEFAULT_SIZE_DP, onHidden: (() -> Unit)? = null) {
        removeNow()
        this.onHidden = onHidden

        val screenW = appContext.resources.displayMetrics.widthPixels
        val screenH = appContext.resources.displayMetrics.heightPixels
        val viewW = minOf(screenW - dp(24), dp(320))
        val viewH = dp(CharacterView.BUBBLE_DP.toInt()) + dp(sizeDp)
        val finalX = (screenW - viewW) / 2
        val finalY = screenH - viewH - dp(BOTTOM_MARGIN_DP)

        val v = CharacterView(appContext).apply {
            this.level = level
            this.line = line
            setOnClickListener { hide() }
        }
        val lp = WindowManager.LayoutParams(
            viewW,
            viewH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // Titik awal animasi masuk (Bagian 5.3): L1/Terlambat ngintip dari bawah,
        // L2-L4 slide dari samping, L5 jatuh dari atas.
        val fromX: Int
        val fromY: Int
        val interpolator: Interpolator
        val duration: Long
        when (level) {
            NagLevel.WORRIED, NagLevel.PANIC -> {
                fromX = -viewW; fromY = finalY; interpolator = DecelerateInterpolator(); duration = 450
            }
            NagLevel.STRESSED -> {
                fromX = screenW; fromY = finalY; interpolator = DecelerateInterpolator(); duration = 450
            }
            NagLevel.CRITICAL -> {
                fromX = finalX; fromY = -viewH; interpolator = BounceInterpolator(); duration = 900
            }
            else -> {
                fromX = finalX; fromY = screenH; interpolator = DecelerateInterpolator(); duration = 500
            }
        }
        lp.x = fromX
        lp.y = fromY

        try {
            windowManager.addView(v, lp)
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal menambahkan overlay", t)
            onHidden?.invoke()
            return
        }
        view = v
        params = lp
        leaving = false

        move(fromX, fromY, finalX, finalY, duration, interpolator, null)
        val hold = durationMs(level)
        hideRunnable = Runnable { hide() }.also { handler.postDelayed(it, hold) }
    }

    /** Keluar dengan animasi turun ke bawah, lalu lepas view. */
    fun hide() {
        val v = view ?: return
        val lp = params ?: return
        if (leaving) return
        leaving = true
        hideRunnable?.let { handler.removeCallbacks(it) }
        val screenH = appContext.resources.displayMetrics.heightPixels
        move(lp.x, lp.y, lp.x, screenH, 300, AccelerateInterpolator()) {
            if (view === v) removeNow()
        }
    }

    private fun move(
        fx: Int, fy: Int, tx: Int, ty: Int, duration: Long,
        interpolator: Interpolator, onEnd: (() -> Unit)?
    ) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).also { a ->
            a.duration = duration
            a.interpolator = interpolator
            a.addUpdateListener {
                val lp = params ?: return@addUpdateListener
                val v = view ?: return@addUpdateListener
                val f = it.animatedValue as Float
                lp.x = fx + ((tx - fx) * f).toInt()
                lp.y = fy + ((ty - fy) * f).toInt()
                try {
                    windowManager.updateViewLayout(v, lp)
                } catch (_: Throwable) {
                    a.cancel()
                }
            }
            if (onEnd != null) {
                a.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = onEnd()
                })
            }
            a.start()
        }
    }

    private fun removeNow() {
        animator?.cancel()
        animator = null
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = null
        val v = view
        view = null
        params = null
        leaving = false
        if (v != null) {
            try {
                windowManager.removeViewImmediate(v)
            } catch (t: Throwable) {
                Log.w(TAG, "Gagal melepas overlay", t)
            }
            val cb = onHidden
            onHidden = null
            cb?.invoke()
        }
    }

    private fun dp(v: Int) = (v * density).toInt()

    companion object {
        private const val TAG = "OverlayManager"
        const val DEFAULT_SIZE_DP = 180
        private const val BOTTOM_MARGIN_DP = 72

        /** Lama karakter tampil per level — Bagian 5.3. */
        fun durationMs(level: NagLevel): Long = when (level) {
            NagLevel.DORMANT, NagLevel.CALM -> 4_000L
            NagLevel.WORRIED -> 6_000L
            NagLevel.STRESSED -> 8_000L
            NagLevel.PANIC -> 10_000L
            NagLevel.CRITICAL -> 15_000L
            NagLevel.OVERDUE -> 5_000L
        }
    }
}
