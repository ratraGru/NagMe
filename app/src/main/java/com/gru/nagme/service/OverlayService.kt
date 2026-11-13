package com.gru.nagme.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gru.nagme.MainActivity
import com.gru.nagme.NagMeApp
import com.gru.nagme.R
import com.gru.nagme.domain.NagLevel
import com.gru.nagme.overlay.OverlayActions
import com.gru.nagme.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service SINGKAT: hidup hanya selama karakter tampil dan suara berbunyi
 * (Bagian 5.4 / B6), lalu berhenti sendiri. Tidak ada notifikasi permanen.
 * Bila overlay tidak bisa tampil (layar terkunci) service hanya bersuara.
 */
class OverlayService : Service() {
    private var manager: OverlayManager? = null
    private var voice: VoiceManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var overlayGone = true
    private var speechGone = true
    private var level = NagLevel.CALM
    private var taskId = TEST_TASK_ID
    private var failSafe: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lv = NagLevel.values().firstOrNull { it.value == intent?.getIntExtra(EXTRA_LEVEL, -1) }
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        val remaining = intent?.getLongExtra(EXTRA_REMAINING_MS, 0L) ?: 0L
        val celebrate = intent?.getBooleanExtra(EXTRA_CELEBRATE, false) ?: false
        val showOverlay = intent?.getBooleanExtra(EXTRA_SHOW_OVERLAY, true) ?: true
        val id = intent?.getIntExtra(EXTRA_TASK_ID, TEST_TASK_ID) ?: TEST_TASK_ID
        if (lv == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        level = lv
        taskId = id

        try {
            ensureChannel(this)
            ServiceCompat.startForeground(
                this, FGS_ID, buildNotification(this, title),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
            val settings = (application as NagMeApp).container.settings
            val line = if (celebrate) LineBank.celebrate() else LineBank.pick(lv, remaining, title)

            failSafe?.let { handler.removeCallbacks(it) }
            failSafe = Runnable { stopSelf() }.also { handler.postDelayed(it, FAILSAFE_MS) }

            vibrate(lv)
            if (showOverlay) {
                val m = manager ?: OverlayManager(this).also { manager = it }
                overlayGone = false
                m.show(
                    lv, line, settings.sizeDp, celebrate,
                    OverlayActions(
                        onDone = { onDone() },
                        onSnooze = { onSnooze() },
                        onOpenApp = { onOpenApp() }
                    )
                ) {
                    overlayGone = true
                    maybeStop()
                }
                if (!m.isShowing()) throw IllegalStateException("overlay tidak tampil")
            } else {
                overlayGone = true
            }
            say(line, if (celebrate) NagLevel.CALM else lv)
        } catch (t: Throwable) {
            // Cadangan: kalau overlay gagal, pengingat tetap sampai lewat notifikasi.
            Log.e(TAG, "Overlay gagal, pakai notifikasi", t)
            if (id != TEST_TASK_ID) NagNotifier.post(this, id, title, text)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /** Ucapkan [line] lewat TTS; service baru boleh berhenti setelah suara selesai. */
    private fun say(line: String, speakLevel: NagLevel) {
        val settings = (application as NagMeApp).container.settings
        val v = voice ?: VoiceManager(this, settings).also { voice = it }
        speechGone = false
        v.speak(line, speakLevel) {
            speechGone = true
            maybeStop()
        }
    }

    private fun maybeStop() {
        if (overlayGone && speechGone) stopSelf()
    }

    private fun onDone() {
        scope.launch {
            val repo = (application as NagMeApp).container.taskRepository
            if (taskId != TEST_TASK_ID) {
                repo.get(taskId.toLong())?.let { repo.setCompleted(it, true) }
            }
            val line = LineBank.celebrate()
            manager?.updateLine(line, true, 3_000L)
            say(line, NagLevel.CALM)
        }
    }

    private fun onSnooze() {
        scope.launch {
            val repo = (application as NagMeApp).container.taskRepository
            var count = 1
            if (taskId != TEST_TASK_ID) {
                repo.get(taskId.toLong())?.let { count = repo.snooze(it) }
            }
            val line = LineBank.snoozeLine(count)
            manager?.updateLine(line, false, 3_500L)
            say(line, level)
        }
    }

    private fun onOpenApp() {
        try {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Tidak bisa membuka app dari overlay", t)
        }
        manager?.hide()
    }

    override fun onDestroy() {
        failSafe?.let { handler.removeCallbacks(it) }
        scope.cancel()
        manager?.hideNow()
        manager = null
        voice?.shutdown()
        voice = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun vibrate(level: NagLevel) {
        if (level != NagLevel.PANIC && level != NagLevel.CRITICAL) return
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!v.hasVibrator()) return
        val pattern = if (level == NagLevel.CRITICAL) longArrayOf(0, 250, 120, 250, 120, 400)
        else longArrayOf(0, 200)
        v.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    companion object {
        private const val TAG = "OverlayService"
        private const val FGS_CHANNEL = "nag_overlay"
        private const val FGS_ID = 4242
        private const val FAILSAFE_MS = 45_000L
        const val TEST_TASK_ID = -1
        const val EXTRA_LEVEL = "level"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_REMAINING_MS = "remaining_ms"
        const val EXTRA_CELEBRATE = "celebrate"
        const val EXTRA_SHOW_OVERLAY = "show_overlay"

        /** Overlay hanya masuk akal bila izin ada, layar menyala, dan tidak terkunci (Bagian 6.6). */
        fun canShow(context: Context): Boolean = whyNot(context) == null

        /** Penjelasan singkat untuk Diagnostik bila overlay tidak bisa tampil. */
        fun whyNot(context: Context): String? {
            if (!Settings.canDrawOverlays(context)) return "no overlay permission"
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!power.isInteractive) return "screen off"
            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (keyguard.isKeyguardLocked) return "screen locked"
            return null
        }

        /**
         * True bila service berhasil diminta jalan (bukan jaminan overlay tampil).
         * [showOverlay] false = hanya suara (layar terkunci / izin overlay tidak ada).
         */
        fun start(
            context: Context,
            level: NagLevel,
            taskId: Int,
            title: String,
            text: String,
            remainingMs: Long,
            showOverlay: Boolean = true,
            celebrate: Boolean = false
        ): Boolean {
            val intent = Intent(context, OverlayService::class.java)
                .putExtra(EXTRA_LEVEL, level.value)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_REMAINING_MS, remainingMs)
                .putExtra(EXTRA_SHOW_OVERLAY, showOverlay)
                .putExtra(EXTRA_CELEBRATE, celebrate)
            return try {
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (t: Throwable) {
                Log.e(TAG, "Tidak bisa menjalankan OverlayService", t)
                false
            }
        }

        private fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                FGS_CHANNEL, context.getString(R.string.fgs_channel_name), NotificationManager.IMPORTANCE_LOW
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private fun buildNotification(context: Context, title: String): Notification =
            NotificationCompat.Builder(context, FGS_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_nag)
                .setContentTitle(title.ifBlank { context.getString(R.string.app_name) })
                .setContentText(context.getString(R.string.fgs_notif_body))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
    }
}
