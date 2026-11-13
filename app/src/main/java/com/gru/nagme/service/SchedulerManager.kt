package com.gru.nagme.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.gru.nagme.data.TaskRepository
import com.gru.nagme.domain.Clock
import com.gru.nagme.domain.NagScheduling
import com.gru.nagme.domain.formatPrecise
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Menghitung timestamp trigger terdekat dari SEMUA tugas aktif lalu mendaftarkan
 * SATU alarm saja (Master Context Bagian 6.1 — ATURAN KRITIS: bukan 1 alarm per tugas per level).
 */
class SchedulerManager(
    context: Context,
    private val repository: TaskRepository,
    private val clock: Clock,
    private val prefs: SchedulerPrefs,
    /** Selisih jam simulasi (Mode Demo); AlarmManager tetap memakai waktu asli. */
    private val offsetMs: () -> Long = { 0L }
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val mutex = Mutex()

    suspend fun scheduleNext() {
        mutex.withLock {
            val now = clock.now()
            val next = repository.getActiveNagTasks()
                .mapNotNull { NagScheduling.nextTriggerEpoch(it, now) }
                .minOrNull()

            if (next == null) {
                alarmManager.cancel(pendingIntent())
                prefs.nextTriggerEpoch = 0L
                Log.d(TAG, "Tidak ada trigger tersisa, alarm dibatalkan")
            } else {
                // Jika sudah jatuh tempo, bunyi sebentar lagi (bukan "sekarang juga") agar tidak looping cepat.
                val at = maxOf(next, now + MIN_DELAY_MS)
                val realAt = maxOf(at - offsetMs(), System.currentTimeMillis() + MIN_DELAY_MS)
                val exact = setAlarm(realAt)
                prefs.nextTriggerEpoch = at
                prefs.lastScheduleInfo =
                    "set ${formatPrecise(now)} -> fire ${formatPrecise(at)} exact=$exact"
                Log.d(TAG, "Alarm tunggal dijadwalkan pada $at (dalam ${(at - now) / 1000} dtk)")
            }
        }
    }

    suspend fun cancelAll() {
        mutex.withLock {
            alarmManager.cancel(pendingIntent())
            prefs.nextTriggerEpoch = 0L
        }
    }

    /** Dipanggil setelah reboot / update app / ganti waktu: hitung ulang dan daftarkan lagi. */
    suspend fun onBootCompleted() = scheduleNext()

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Mengembalikan true bila alarm exact yang terpasang, false bila terpaksa alarm biasa. */
    private fun setAlarm(at: Long): Boolean {
        val pi = pendingIntent()
        if (canScheduleExact()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                return true
            } catch (e: SecurityException) {
                Log.w(TAG, "Izin alarm exact dicabut mendadak, pakai alarm biasa", e)
            }
        }
        // Izin ditolak: kurang presisi (bisa terlambat beberapa menit), banner peringatan di app.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        return false
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(appContext, AlarmReceiver::class.java).setAction(ACTION_NAG_ALARM)
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_NAG_ALARM = "com.gru.nagme.action.NAG_ALARM"
        private const val REQUEST_CODE = 1001
        private const val MIN_DELAY_MS = 2_000L
        private const val TAG = "SchedulerManager"
    }
}
