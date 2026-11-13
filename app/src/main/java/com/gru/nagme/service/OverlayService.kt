package com.gru.nagme.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.app.KeyguardManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gru.nagme.R
import com.gru.nagme.domain.NagLevel
import com.gru.nagme.overlay.OverlayManager

/**
 * Foreground service SINGKAT: hidup hanya selama karakter tampil (Bagian 5.4 / B6),
 * lalu berhenti sendiri. Tidak ada notifikasi permanen.
 */
class OverlayService : Service() {
    private var manager: OverlayManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val level = NagLevel.values().firstOrNull { it.value == intent?.getIntExtra(EXTRA_LEVEL, -1) }
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        val taskId = intent?.getIntExtra(EXTRA_TASK_ID, 0) ?: 0
        if (level == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            ensureChannel(this)
            ServiceCompat.startForeground(
                this, FGS_ID, buildNotification(this, title),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
            val m = manager ?: OverlayManager(this).also { manager = it }
            vibrate(level)
            m.show(level, text) { stopSelf() }
            if (!m.isShowing()) throw IllegalStateException("overlay tidak tampil")
        } catch (t: Throwable) {
            // Cadangan: kalau overlay gagal, pengingat tetap sampai lewat notifikasi.
            Log.e(TAG, "Overlay gagal, pakai notifikasi", t)
            if (taskId != TEST_TASK_ID) NagNotifier.post(this, taskId, title, text)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        manager?.hide()
        manager = null
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
        const val TEST_TASK_ID = -1
        const val EXTRA_LEVEL = "level"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_TASK_ID = "task_id"

        /** Overlay hanya masuk akal bila izin ada, layar menyala, dan tidak terkunci (Bagian 6.6). */
        fun canShow(context: Context): Boolean {
            if (!Settings.canDrawOverlays(context)) return false
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            return power.isInteractive && !keyguard.isKeyguardLocked
        }

        /** Penjelasan singkat untuk Diagnostik bila overlay tidak bisa tampil. */
        fun whyNot(context: Context): String? {
            if (!Settings.canDrawOverlays(context)) return "no overlay permission"
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!power.isInteractive) return "screen off"
            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (keyguard.isKeyguardLocked) return "screen locked"
            return null
        }

        /** True bila service berhasil diminta jalan (bukan jaminan overlay tampil). */
        fun start(context: Context, level: NagLevel, taskId: Int, title: String, text: String): Boolean {
            val intent = Intent(context, OverlayService::class.java)
                .putExtra(EXTRA_LEVEL, level.value)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
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
