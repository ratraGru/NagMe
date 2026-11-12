package com.gru.nagme

import android.content.Context
import androidx.room.Room
import com.gru.nagme.data.AppDatabase
import com.gru.nagme.data.TaskRepository
import com.gru.nagme.domain.Clock
import com.gru.nagme.domain.RealClock

/** DI manual (tanpa Hilt) — Master Context Bagian 7.2. */
class AppContainer(context: Context) {
    val clock: Clock = RealClock

    private val database: AppDatabase = Room
        .databaseBuilder(context.applicationContext, AppDatabase::class.java, "nagme.db")
        .fallbackToDestructiveMigration() // boleh selama demo (Bagian 8)
        .build()

    val taskRepository: TaskRepository = TaskRepository(database.taskDao(), clock)
}
