package com.gru.nagme.service

import android.content.Context

/** Catatan kecil untuk layar Diagnostik: kapan alarm tunggal dijadwalkan berbunyi. */
class SchedulerPrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nagme_scheduler", Context.MODE_PRIVATE)

    var nextTriggerEpoch: Long
        get() = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        set(value) {
            prefs.edit().putLong(KEY_NEXT_TRIGGER, value).apply()
        }

    private companion object {
        const val KEY_NEXT_TRIGGER = "next_trigger_epoch"
    }
}
