package com.gru.nagme.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gru.nagme.NagMeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Menerima broadcast dari AlarmManager (alarm tunggal). Isinya ada di [AlarmProcessor]. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as NagMeApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                AlarmProcessor.runAndReschedule(app)
            } finally {
                pending.finish()
            }
        }
    }
}
