package com.gru.nagme.domain

import com.gru.nagme.data.TaskEntity
import com.gru.nagme.data.TaskStatus

/**
 * Logika "kapan karakter harus muncul" — fungsi murni, mudah diuji.
 * Mengikuti Master Context Bagian 5.3, 6.1, 6.4, 6.7.
 */
object NagScheduling {
    private const val HOUR_MS = 3_600_000L

    /** Snooze menunda 10 menit (Bagian 6.4). */
    const val SNOOZE_MS = 10 * 60_000L

    /** Sisa waktu (ms) tepat saat sebuah level mulai berlaku. */
    fun thresholdMs(level: NagLevel): Long = when (level) {
        NagLevel.DORMANT -> Long.MAX_VALUE
        NagLevel.CALM -> 72 * HOUR_MS
        NagLevel.WORRIED -> 24 * HOUR_MS
        NagLevel.STRESSED -> 12 * HOUR_MS
        NagLevel.PANIC -> 6 * HOUR_MS
        NagLevel.CRITICAL -> 3 * HOUR_MS
        NagLevel.OVERDUE -> 0L
    }

    /** Waktu (epoch ms) sebuah level mulai berlaku untuk deadline tertentu. */
    fun triggerEpoch(deadlineEpoch: Long, level: NagLevel): Long =
        deadlineEpoch - thresholdMs(level)

    /**
     * Level yang harus ditampilkan SEKARANG untuk tugas ini, atau null kalau belum waktunya.
     * - Tugas selesai / nag dimatikan => null.
     * - Sedang snooze => null; snooze habis => muncul lagi di level saat ini.
     * - Selain itu muncul hanya jika level saat ini lebih tinggi dari last_triggered_level
     *   (1 tugas tidak double-trigger pada level yang sama).
     */
    fun dueLevel(task: TaskEntity, now: Long): NagLevel? {
        if (task.status != TaskStatus.ACTIVE || !task.nagEnabled) return null
        if (task.snoozeUntilEpoch > now) return null
        val current = NagLevelResolver.resolve(task.deadlineEpoch, now)
        if (current == NagLevel.DORMANT) return null
        if (task.snoozeUntilEpoch > 0L) return current
        return if (current.value > task.lastTriggeredLevel) current else null
    }

    /**
     * Waktu trigger berikutnya untuk tugas ini (epoch ms), atau null kalau tidak ada lagi.
     * Hasil bisa <= now kalau ada yang sudah jatuh tempo; penjadwal yang menanganinya.
     */
    @Suppress("UNUSED_PARAMETER")
    fun nextTriggerEpoch(task: TaskEntity, now: Long): Long? {
        if (task.status != TaskStatus.ACTIVE || !task.nagEnabled) return null
        if (task.snoozeUntilEpoch > 0L) return task.snoozeUntilEpoch
        return NagLevel.entries
            .filter { it != NagLevel.DORMANT && it.value > task.lastTriggeredLevel }
            .minOfOrNull { triggerEpoch(task.deadlineEpoch, it) }
    }
}
