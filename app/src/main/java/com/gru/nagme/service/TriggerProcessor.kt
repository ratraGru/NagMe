package com.gru.nagme.service

import com.gru.nagme.data.TaskEntity
import com.gru.nagme.data.TaskRepository
import com.gru.nagme.domain.Clock
import com.gru.nagme.domain.NagLevel
import com.gru.nagme.domain.NagLevelResolver
import com.gru.nagme.domain.NagScheduling
import com.gru.nagme.domain.formatPrecise

data class DueTrigger(val task: TaskEntity, val level: NagLevel)

/** [detail] = catatan teknis singkat (untuk layar Diagnostik) kenapa tiap tugas jatuh tempo atau tidak. */
data class ProcessResult(val due: List<DueTrigger>, val detail: String)

/** Dipanggil saat alarm berbunyi: cari tugas yang jatuh tempo, catat level-nya, kembalikan daftarnya. */
class TriggerProcessor(
    private val repository: TaskRepository,
    private val clock: Clock
) {
    suspend fun processDue(): ProcessResult {
        val now = clock.now()
        // Toleransi: alarm yang berbunyi sedikit lebih awal dari jadwalnya tetap dianggap jatuh tempo.
        // "cur" di catatan memakai waktu asli, jadi alarm yang bunyi kecepatan terlihat: cur=L5 tetapi due=6.
        val effectiveNow = now + EARLY_SLACK_MS
        val tasks = repository.getActiveNagTasks()
        val due = ArrayList<DueTrigger>()
        val detail = StringBuilder("now=").append(formatPrecise(now))
            .append(" (+").append(EARLY_SLACK_MS).append("ms slack)")

        if (tasks.isEmpty()) detail.append("\n(no active tasks)")

        for (task in tasks) {
            val level = NagScheduling.dueLevel(task, effectiveNow)
            if (detail.count { it == '\n' } < MAX_DETAIL_TASKS) {
                detail.append('\n')
                    .append(task.title.take(12))
                    .append(": D=").append(formatPrecise(task.deadlineEpoch))
                    .append(" cur=L").append(NagLevelResolver.resolve(task.deadlineEpoch, now).value)
                    .append(" last=L").append(task.lastTriggeredLevel)
                    .append(" due=").append(level?.value?.toString() ?: "-")
            }
            if (level == null) continue
            repository.markTriggered(task, level.value)
            due += DueTrigger(task, level)
        }
        return ProcessResult(due, detail.toString())
    }

    private companion object {
        const val MAX_DETAIL_TASKS = 4
        const val EARLY_SLACK_MS = 5_000L
    }
}
