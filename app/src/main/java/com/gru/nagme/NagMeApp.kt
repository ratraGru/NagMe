package com.gru.nagme

import android.app.Application
import com.gru.nagme.service.NagNotifier
import kotlinx.coroutines.launch

class NagMeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NagNotifier.ensureChannel(this)
        // Sinkronkan alarm tunggal setiap proses app mulai (mis. setelah update / dibuka lagi).
        container.appScope.launch { container.scheduler.scheduleNext() }
    }
}
