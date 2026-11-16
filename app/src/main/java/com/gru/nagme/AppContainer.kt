package com.gru.nagme

import android.content.Context
import androidx.room.Room
import com.gru.nagme.data.AppDatabase
import com.gru.nagme.data.TaskRepository
import com.gru.nagme.domain.Clock
import com.gru.nagme.domain.SimClock
import com.gru.nagme.data.SettingsPrefs
import com.gru.nagme.service.SchedulerManager
import com.gru.nagme.service.SchedulerPrefs
import com.gru.nagme.service.TriggerProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** DI manual (tanpa Hilt) — Master Context Bagian 7.2. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settings = SettingsPrefs(appContext)
    val clock: Clock = SimClock { settings.simOffsetMs }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase = Room
        .databaseBuilder(appContext, AppDatabase::class.java, "nagme.db")
        .fallbackToDestructiveMigration() // boleh selama demo (Bagian 8)
        .build()

    val schedulerPrefs = SchedulerPrefs(appContext)

    // taskRepository & scheduler saling butuh; lazy + lambda memutus lingkarannya.
    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao(), clock) { scheduler.scheduleNext() }
    }

    val scheduler: SchedulerManager by lazy {
        SchedulerManager(appContext, taskRepository, clock, schedulerPrefs) { settings.simOffsetMs }
    }

    val triggerProcessor: TriggerProcessor by lazy {
        TriggerProcessor(taskRepository, clock)
    }
}
