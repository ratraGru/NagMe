package com.gru.nagme.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gru.nagme.MainActivity
import com.gru.nagme.R
import com.gru.nagme.domain.NagLevel

/**
 * Notifikasi pengingat. Di Hari 4 ini satu-satunya "wujud" pengingat; mulai Hari 5 jadi
 * cadangan saat overlay tidak bisa tampil (izin ditolak / layar terkunci — Bagian 6.6).
 */
object NagNotifier {
    const val CHANNEL_ID = "nag_reminders"
    private const val TAG = "NagNotifier"
    private const val TEST_NOTIFICATION_ID = Int.MAX_VALUE

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_desc)
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** False bila saluran notifikasi dimatikan user / sistem (notifikasi tidak akan terlihat). */
    fun isChannelEnabled(context: Context): Boolean {
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Notifikasi uji langsung (tanpa alarm) untuk memastikan jalur notifikasi berfungsi. */
    fun showTest(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nag)
            .setContentTitle(context.getString(R.string.notif_test_title))
            .setContentText(context.getString(R.string.notif_test_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(TEST_NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Tidak punya izin menampilkan notifikasi uji", e)
            false
        }
    }

    /** True bila notifikasi benar-benar dikirim ke sistem. */
    fun show(context: Context, trigger: DueTrigger, now: Long): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifikasi dimatikan, pengingat untuk '${trigger.task.title}' tidak ditampilkan")
            return false
        }

        return post(context, trigger.task.id.toInt(), trigger.task.title, bodyText(context, trigger, now))
    }

    /** Teks isi pengingat sesuai level (dipakai notifikasi, overlay, dan nanti TTS). */
    fun bodyText(context: Context, trigger: DueTrigger, now: Long): String =
        context.getString(
            bodyRes(trigger.level),
            formatRemainingPlain(context, trigger.task.deadlineEpoch - now)
        )

    /** Kirim notifikasi pengingat biasa. True bila diteruskan ke sistem. */
    fun post(context: Context, id: Int, title: String, text: String): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val openApp = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nag)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        return try {
            manager.notify(id, notification)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Tidak punya izin menampilkan notifikasi", e)
            false
        }
    }

    private fun bodyRes(level: NagLevel): Int = when (level) {
        NagLevel.DORMANT, NagLevel.CALM -> R.string.notif_l1
        NagLevel.WORRIED -> R.string.notif_l2
        NagLevel.STRESSED -> R.string.notif_l3
        NagLevel.PANIC -> R.string.notif_l4
        NagLevel.CRITICAL -> R.string.notif_l5
        NagLevel.OVERDUE -> R.string.notif_l6
    }
}

/** Sisa waktu sebagai teks pendek (di luar Compose), mis. "2h 59m" / "2 jam 59 mnt". */
fun formatRemainingPlain(context: Context, remainingMs: Long): String {
    val totalMinutes = remainingMs.coerceAtLeast(0L) / 60_000L
    val days = (totalMinutes / 1_440L).toInt()
    val hours = ((totalMinutes % 1_440L) / 60L).toInt()
    val minutes = (totalMinutes % 60L).toInt()
    return when {
        days > 0 -> context.getString(R.string.remaining_dh, days, hours)
        hours > 0 -> context.getString(R.string.remaining_hm, hours, minutes)
        else -> context.getString(R.string.remaining_m, minutes)
    }
}
