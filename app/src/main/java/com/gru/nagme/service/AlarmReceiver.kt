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
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val due = container.triggerProcessor.processDue()
                val now = container.clock.now()
                due.forEach { NagNotifier.show(app, it, now) }
                Log.d(TAG, "Alarm diproses, ${due.size} tugas jatuh tempo")
            } catch (t: Throwable) {
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
