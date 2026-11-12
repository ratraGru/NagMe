package com.gru.nagme.data

import com.gru.nagme.domain.Clock
import com.gru.nagme.domain.NagLevelResolver
import kotlinx.coroutines.flow.Flow

/**
 * Semua akses data tugas lewat sini (Bagian 7.1 A2).
 * Aturan 6.7: tugas baru / deadline diedit => last_triggered_level = level saat ini,
 * supaya level yang sedang berjalan tidak langsung memicu karakter.
 */
class TaskRepository(
    private val dao: TaskDao,
    private val clock: Clock
) {
    fun observeAll(): Flow<List<TaskEntity>> = dao.observeAll()

    fun observeBetween(startEpoch: Long, endEpoch: Long): Flow<List<TaskEntity>> =
        dao.observeBetween(startEpoch, endEpoch)

    suspend fun get(id: Long): TaskEntity? = dao.getById(id)

    suspend fun add(
        title: String,
        description: String?,
        deadlineEpoch: Long,
        category: Int,
        nagEnabled: Boolean
    ): Long {
        val now = clock.now()
        return dao.insert(
            TaskEntity(
                title = title.trim(),
                description = description?.trim()?.ifBlank { null },
                deadlineEpoch = deadlineEpoch,
                createdAt = now,
                updatedAt = now,
                category = category,
                nagEnabled = nagEnabled,
                lastTriggeredLevel = NagLevelResolver.resolve(deadlineEpoch, now).value
            )
        )
    }

    suspend fun edit(
        old: TaskEntity,
        title: String,
        description: String?,
        deadlineEpoch: Long,
        category: Int,
        nagEnabled: Boolean
    ) {
        val now = clock.now()
        val deadlineChanged = old.deadlineEpoch != deadlineEpoch
        dao.update(
            old.copy(
                title = title.trim(),
                description = description?.trim()?.ifBlank { null },
                deadlineEpoch = deadlineEpoch,
                updatedAt = now,
                category = category,
                nagEnabled = nagEnabled,
                snoozeCount = if (deadlineChanged) 0 else old.snoozeCount,
                snoozeUntilEpoch = if (deadlineChanged) 0 else old.snoozeUntilEpoch,
                lastTriggeredLevel = if (deadlineChanged) {
                    NagLevelResolver.resolve(deadlineEpoch, now).value
                } else {
                    old.lastTriggeredLevel
                }
            )
        )
    }

    suspend fun delete(task: TaskEntity) = dao.delete(task)

    suspend fun setCompleted(task: TaskEntity, completed: Boolean) {
        val now = clock.now()
        dao.update(
            task.copy(
                status = if (completed) TaskStatus.DONE else TaskStatus.ACTIVE,
                updatedAt = now,
                snoozeCount = 0,
                snoozeUntilEpoch = 0,
                lastTriggeredLevel = if (completed) {
                    task.lastTriggeredLevel
                } else {
                    NagLevelResolver.resolve(task.deadlineEpoch, now).value
                }
            )
        )
    }
}
