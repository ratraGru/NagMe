package com.gru.nagme.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gru.nagme.NagMeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Menerima broadcast dari AlarmManager (alarm tunggal). Alur: Bagian 6.1 langkah 4-9. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as NagMeApp
        val container = app.container
        val prefs = container.schedulerPrefs
        val pending = goAsync()

        // Catat segera bahwa alarm SAMPAI ke app (untuk layar Diagnostik).
        prefs.lastAlarmEpoch = container.clock.now()
        prefs.lastDueCount = -1
        prefs.lastPostedCount = -1
        prefs.lastAlarmDetail = ""

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val result = container.triggerProcessor.processDue()
                val now = container.clock.now()
                var posted = 0
                result.due.forEach { if (NagNotifier.show(app, it, now)) posted++ }
                prefs.lastDueCount = result.due.size
                prefs.lastPostedCount = posted
                prefs.lastAlarmDetail = result.detail
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
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AlarmReceiver"
    }
}
