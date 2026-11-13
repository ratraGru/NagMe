package com.gru.nagme.service

import android.content.Context

/** Catatan kecil untuk layar Diagnostik: jadwal alarm tunggal dan hasil alarm terakhir. */
class SchedulerPrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nagme_scheduler", Context.MODE_PRIVATE)

    /** Kapan alarm tunggal dijadwalkan berbunyi (0 = tidak ada). */
    var nextTriggerEpoch: Long
        get() = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        set(value) {
            prefs.edit().putLong(KEY_NEXT_TRIGGER, value).apply()
        }

    /** Kapan AlarmReceiver terakhir dijalankan (0 = belum pernah). */
    var lastAlarmEpoch: Long
        get() = prefs.getLong(KEY_LAST_ALARM, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_ALARM, value).apply()
        }

    /** Jumlah tugas jatuh tempo pada alarm terakhir (-1 = gagal diproses / belum ada). */
    var lastDueCount: Int
        get() = prefs.getInt(KEY_LAST_DUE, -1)
        set(value) {
            prefs.edit().putInt(KEY_LAST_DUE, value).apply()
        }

    /** Jumlah notifikasi yang benar-benar terkirim pada alarm terakhir. */
    var lastPostedCount: Int
        get() = prefs.getInt(KEY_LAST_POSTED, -1)
        set(value) {
            prefs.edit().putInt(KEY_LAST_POSTED, value).apply()
        }

    /** Catatan teknis pengecekan alarm terakhir (per tugas: deadline, level, due). */
    var lastAlarmDetail: String
        get() = prefs.getString(KEY_LAST_DETAIL, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_LAST_DETAIL, value).apply()
        }

    /** Catatan penjadwalan alarm terakhir (kapan dipasang, kapan akan berbunyi, exact atau tidak). */
    var lastScheduleInfo: String
        get() = prefs.getString(KEY_LAST_SCHEDULE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_LAST_SCHEDULE, value).apply()
        }

    private companion object {
        const val KEY_LAST_DETAIL = "last_alarm_detail"
        const val KEY_LAST_SCHEDULE = "last_schedule_info"
        const val KEY_NEXT_TRIGGER = "next_trigger_epoch"
        const val KEY_LAST_ALARM = "last_alarm_epoch"
        const val KEY_LAST_DUE = "last_due_count"
        const val KEY_LAST_POSTED = "last_posted_count"
    }
}
