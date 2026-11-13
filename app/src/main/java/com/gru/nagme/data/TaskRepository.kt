package com.gru.nagme.data

import com.gru.nagme.domain.Clock
import com.gru.nagme.domain.NagLevelResolver
import com.gru.nagme.domain.NagScheduling
import kotlinx.coroutines.flow.Flow

/**
 * Semua akses data tugas lewat sini (Bagian 7.1 A2).
 * Aturan 6.7: tugas baru / deadline diedit => last_triggered_level = level saat ini,
 * supaya level yang sedang berjalan tidak langsung memicu karakter.
 *
 * [onChanged] dipanggil setelah user membuat/mengubah/menghapus/menyelesaikan tugas,
 * dipakai untuk menghitung ulang 1 alarm terdekat (Bagian 6.1).
 */
class TaskRepository(
    private val dao: TaskDao,
    private val clock: Clock,
    private val onChanged: suspend () -> Unit = {}
) {
    fun observeAll(): Flow<List<TaskEntity>> = dao.observeAll()

    fun observeBetween(startEpoch: Long, endEpoch: Long): Flow<List<TaskEntity>> =
        dao.observeBetween(startEpoch, endEpoch)

    suspend fun get(id: Long): TaskEntity? = dao.getById(id)

    /** Tugas aktif dengan nag menyala — bahan hitung alarm berikutnya. */
    suspend fun getActiveNagTasks(): List<TaskEntity> = dao.getActiveNagTasks()

    suspend fun add(
        title: String,
        description: String?,
        deadlineEpoch: Long,
        category: Int,
        nagEnabled: Boolean
    ): Long {
        val now = clock.now()
        val id = dao.insert(
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
        onChanged()
        return id
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
        onChanged()
    }

    /** Snooze +10 menit (Bagian 6.4). Mengembalikan hitungan snooze yang baru. */
    suspend fun snooze(task: TaskEntity): Int {
        val now = clock.now()
        val count = task.snoozeCount + 1
        dao.update(
            task.copy(snoozeCount = count, snoozeUntilEpoch = now + NagScheduling.SNOOZE_MS, updatedAt = now)
        )
        onChanged()
        return count
    }

    /** Mode Demo: hapus semua tugas. */
    suspend fun deleteAll() {
        dao.deleteAll()
        onChanged()
    }

    suspend fun delete(task: TaskEntity) {
        dao.delete(task)
        onChanged()
    }

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
        onChanged()
    }

    /**
     * Dipanggil penerima alarm setelah sebuah level ditampilkan: catat level terakhir
     * dan bersihkan snooze yang sudah habis. TIDAK memanggil [onChanged] — penerima alarm
     * menghitung ulang alarm sekali di akhir.
     */
    suspend fun markTriggered(task: TaskEntity, level: Int) {
        dao.update(task.copy(lastTriggeredLevel = level, snoozeUntilEpoch = 0))
    }
}
