package com.gru.nagme.service

import android.util.Log
import com.gru.nagme.NagMeApp

/**
 * Isi alarm (Bagian 6.1 langkah 5-9). Dipakai AlarmReceiver dan tombol "Majukan waktu" di Mode Demo.
 * Level tertinggi tampil sebagai karakter + suara; sisanya (atau semuanya bila overlay tidak bisa
 * tampil karena izin / layar terkunci) lewat notifikasi — Bagian 6.6.
 */
object AlarmProcessor {
    private const val TAG = "AlarmProcessor"

    suspend fun runAndReschedule(app: NagMeApp) {
        val container = app.container
        val prefs = container.schedulerPrefs
        val settings = container.settings

        prefs.lastAlarmEpoch = container.clock.now()
        prefs.lastDueCount = -1
        prefs.lastPostedCount = -1
        prefs.lastAlarmDetail = ""

        try {
            val result = container.triggerProcessor.processDue()
            val now = container.clock.now()
            var posted = 0
            var note: String
            if (!settings.masterEnabled) {
                note = "master switch off"
            } else {
                val shown = result.due
                    .filter { settings.isLevelEnabled(it.level) }
                    .sortedByDescending { it.level.value }
                val whyNot = OverlayService.whyNot(app)
                note = whyNot?.let { "overlay: skipped ($it)" } ?: "overlay: -"
                if (shown.size < result.due.size) note += " (${result.due.size - shown.size} level off)"
                shown.forEachIndexed { index, trigger ->
                    var handled = false
                    if (index == 0) {
                        val withOverlay = whyNot == null
                        val started = OverlayService.start(
                            app, trigger.level, trigger.task.id.toInt(), trigger.task.title,
                            NagNotifier.bodyText(app, trigger, now),
                            trigger.task.deadlineEpoch - now, withOverlay
                        )
                        note = when {
                            started && withOverlay -> "overlay: started L${trigger.level.value}"
                            started -> "$note; voice only"
                            else -> "overlay: start FAILED"
                        }
                        handled = started && withOverlay
                    }
                    if (!handled && NagNotifier.show(app, trigger, now)) posted++
                }
            }
            prefs.lastDueCount = result.due.size
            prefs.lastPostedCount = posted
            prefs.lastAlarmDetail = result.detail + "\n" + note
            Log.d(TAG, "Alarm diproses, ${result.due.size} jatuh tempo, $posted notifikasi terkirim")
        } catch (t: Throwable) {
            prefs.lastAlarmDetail = "ERROR: ${t.javaClass.simpleName}: ${t.message}"
            Log.e(TAG, "Gagal memproses alarm", t)
        } finally {
            try {
                container.scheduler.scheduleNext()
            } catch (t: Throwable) {
                Log.e(TAG, "Gagal menjadwalkan alarm berikutnya", t)
            }
        }
    }
}
