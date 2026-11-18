package com.gru.nagme.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gru.nagme.MainActivity
import com.gru.nagme.NagMeApp
import com.gru.nagme.R
import kotlinx.coroutines.launch

/**
 * "Penjaga latar": foreground service ringan dengan notifikasi senyap, supaya proses NagMe
 * tidak mudah dimatikan sistem saat app ditutup dari recent (cara yang sama dipakai app chat).
 * Bisa dimatikan di Pengaturan. Alarm tetap satu (AlarmManager); service ini hanya menjaga proses
 * hidup dan menjadwalkan ulang alarm bila app digeser dari recent.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            createChannel(this)
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(this),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
            rescheduleAlarm()
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal menjalankan penjaga latar", t)
            stopSelf()
        }
        return START_STICKY
    }

    // App digeser dari recent: pastikan alarm tetap terdaftar dan service tetap hidup.
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        rescheduleAlarm()
    }

    private fun rescheduleAlarm() {
        val container = (application as NagMeApp).container
        container.appScope.launch { container.scheduler.scheduleNext() }
    }

    companion object {
        private const val TAG = "KeepAlive"
        private const val CHANNEL_ID = "nag_keepalive"
        private const val NOTIF_ID = 4101

        /** Menyalakan/mematikan service sesuai pengaturan. Aman dipanggil kapan saja. */
        fun sync(context: Context) {
            val app = context.applicationContext
            val enabled = (app as NagMeApp).container.settings.keepAliveEnabled
            if (enabled) start(app) else stop(app)
        }

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, KeepAliveService::class.java)
                )
            } catch (t: Throwable) {
                // Mis. dipanggil saat app di latar dan sistem melarang; tidak fatal.
                Log.w(TAG, "Tidak bisa menjalankan penjaga latar sekarang", t)
            }
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, KeepAliveService::class.java)
            )
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.keepalive_channel),
                    NotificationManager.IMPORTANCE_MIN
                ).apply { setShowBadge(false) }
            )
        }

        private fun buildNotification(context: Context) = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nag)
            .setContentTitle(context.getString(R.string.keepalive_title))
            .setContentText(context.getString(R.string.keepalive_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }
}
