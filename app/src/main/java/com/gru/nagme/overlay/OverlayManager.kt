package com.gru.nagme.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.gru.nagme.R
import com.gru.nagme.data.SettingsPrefs
import com.gru.nagme.domain.NagLevel

/** Aksi di mini menu (tekan lama pada karakter) — Bagian 5.4. */
class OverlayActions(
    val onDone: () -> Unit,
    val onSnooze: () -> Unit,
    val onOpenApp: () -> Unit
)

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

    private var root: FrameLayout? = null
    private var character: CharacterView? = null
    private var menu: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var animator: ValueAnimator? = null
    private var hideRunnable: Runnable? = null
    private var onHidden: (() -> Unit)? = null
    private var leaving = false

    fun isShowing(): Boolean = root != null

    /** Tampilkan karakter. [onHidden] dipanggil setelah view benar-benar dilepas. */
    fun show(
        level: NagLevel,
        line: String,
        sizeDp: Int = DEFAULT_SIZE_DP,
        celebrate: Boolean = false,
        actions: OverlayActions? = null,
        onHidden: (() -> Unit)? = null
    ) {
        this.onHidden = null // overlay lama diganti, jangan panggil callback-nya
        removeNow()
        this.onHidden = onHidden

        val screenW = appContext.resources.displayMetrics.widthPixels
        val screenH = appContext.resources.displayMetrics.heightPixels
        val viewW = minOf(screenW - dp(24), dp(320))
        val viewH = dp(CharacterView.BUBBLE_DP.toInt()) + dp(sizeDp)
        val finalX = (screenW - viewW) / 2
        val finalY = screenH - viewH - dp(BOTTOM_MARGIN_DP)

        val cv = CharacterView(appContext).apply {
            this.level = level
            this.celebrate = celebrate
            this.line = line
            this.style = SettingsPrefs(appContext).characterStyle
            setOnClickListener { hide() }
        }
        val frame = FrameLayout(appContext)
        frame.addView(cv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (actions != null) {
            val m = buildMenu(actions)
            frame.addView(
                m,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(CharacterView.BUBBLE_DP.toInt()) - dp(10)
                ).apply { setMargins(dp(4), dp(2), dp(4), 0) }
            )
            menu = m
            cv.setOnLongClickListener { showMenu(); true }
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
        when {
            celebrate -> {
                fromX = finalX; fromY = screenH; interpolator = BounceInterpolator(); duration = 600
            }
            level == NagLevel.WORRIED || level == NagLevel.PANIC -> {
                fromX = -viewW; fromY = finalY; interpolator = DecelerateInterpolator(); duration = 450
            }
            level == NagLevel.STRESSED -> {
                fromX = screenW; fromY = finalY; interpolator = DecelerateInterpolator(); duration = 450
            }
            level == NagLevel.CRITICAL -> {
                fromX = finalX; fromY = -viewH; interpolator = BounceInterpolator(); duration = 900
            }
            else -> {
                fromX = finalX; fromY = screenH; interpolator = DecelerateInterpolator(); duration = 500
            }
        }
        lp.x = fromX
        lp.y = fromY

        try {
            windowManager.addView(frame, lp)
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal menambahkan overlay", t)
            menu = null
            val cb = this.onHidden
            this.onHidden = null
            cb?.invoke()
            return
        }
        root = frame
        character = cv
        params = lp
        leaving = false

        move(fromX, fromY, finalX, finalY, duration, interpolator, null)
        scheduleHide(if (celebrate) CELEBRATE_MS else durationMs(level))
    }

    /** Ganti teks balon / ekspresi di tempat (mis. jawaban snooze), lalu tutup setelah [holdMs]. */
    fun updateLine(line: String, celebrate: Boolean, holdMs: Long) {
        val cv = character ?: return
        cv.celebrate = celebrate
        cv.line = line
        menu?.visibility = View.GONE
        scheduleHide(holdMs)
    }

    /** Keluar dengan animasi turun ke bawah, lalu lepas view. */
    fun hide() {
        val v = root ?: return
        val lp = params ?: return
        if (leaving) return
        leaving = true
        hideRunnable?.let { handler.removeCallbacks(it) }
        val screenH = appContext.resources.displayMetrics.heightPixels
        move(lp.x, lp.y, lp.x, screenH, 300, AccelerateInterpolator()) {
            if (root === v) removeNow()
        }
    }

    private fun scheduleHide(delayMs: Long) {
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = Runnable { hide() }.also { handler.postDelayed(it, delayMs) }
    }

    private fun showMenu() {
        menu?.visibility = View.VISIBLE
        scheduleHide(MENU_MS)
    }

    private fun buildMenu(actions: OverlayActions): LinearLayout {
        val row = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(0xFFFFFBF2.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(2), 0xFF5A4A42.toInt())
            }
        }
        fun button(textRes: Int, onClick: () -> Unit) = TextView(appContext).apply {
            text = appContext.getString(textRes)
            textSize = 14f
            setTextColor(0xFF3B302B.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { onClick() }
        }
        val lpBtn = { LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        row.addView(button(R.string.overlay_menu_done) { actions.onDone() }, lpBtn())
        row.addView(button(R.string.overlay_menu_snooze) { actions.onSnooze() }, lpBtn())
        row.addView(button(R.string.overlay_menu_open) { actions.onOpenApp() }, lpBtn())
        return row
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
                val v = root ?: return@addUpdateListener
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
        val v = root
        root = null
        character = null
        menu = null
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

    /** Lepas langsung tanpa animasi (service dihentikan). */
    fun hideNow() = removeNow()

    private fun dp(v: Int) = (v * density).toInt()

    companion object {
        private const val TAG = "OverlayManager"
        const val DEFAULT_SIZE_DP = 180
        private const val BOTTOM_MARGIN_DP = 72
        private const val CELEBRATE_MS = 3_000L
        private const val MENU_MS = 8_000L

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
