package com.gru.nagme.service

import com.gru.nagme.data.TaskEntity
import com.gru.nagme.data.TaskRepository
import com.gru.nagme.domain.Clock
import com.gru.nagme.domain.NagLevel
import com.gru.nagme.domain.NagScheduling

data class DueTrigger(val task: TaskEntity, val level: NagLevel)

/** Dipanggil saat alarm berbunyi: cari tugas yang jatuh tempo, catat level-nya, kembalikan daftarnya. */
class TriggerProcessor(
    private val repository: TaskRepository,
    private val clock: Clock
) {
    suspend fun processDue(): List<DueTrigger> {
        val now = clock.now()
        val due = ArrayList<DueTrigger>()
        for (task in repository.getActiveNagTasks()) {
            val level = NagScheduling.dueLevel(task, now) ?: continue
            repository.markTriggered(task, level.value)
            due += DueTrigger(task, level)
        }
        return due
    }
}
