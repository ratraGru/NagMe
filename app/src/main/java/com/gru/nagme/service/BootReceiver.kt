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

/**
 * Menjadwalkan ulang alarm setelah reboot, update app, perubahan jam/zona waktu,
 * atau saat izin alarm exact diberikan (Bagian 6.2).
 * Receiver ini TIDAK menjalankan foreground service, hanya mendaftarkan alarm.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as NagMeApp
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                Log.d(TAG, "Menjadwalkan ulang karena ${intent.action}")
                app.container.scheduler.onBootCompleted()
            } catch (t: Throwable) {
                Log.e(TAG, "Gagal menjadwalkan ulang", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
