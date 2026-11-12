package com.gru.nagme.domain

enum class NagLevel(val value: Int) {
    DORMANT(0),
    CALM(1),
    WORRIED(2),
    STRESSED(3),
    PANIC(4),
    CRITICAL(5),
    OVERDUE(6)
}

/** Penentuan level — Master Context Bagian 5.3 & 6.3 (BAKU). */
object NagLevelResolver {
    private const val HOUR_MS = 3_600_000L

    fun resolve(deadlineEpoch: Long, now: Long): NagLevel {
        val remaining = deadlineEpoch - now
        return when {
            remaining <= 0L -> NagLevel.OVERDUE
            remaining <= 3 * HOUR_MS -> NagLevel.CRITICAL
            remaining <= 6 * HOUR_MS -> NagLevel.PANIC
            remaining <= 12 * HOUR_MS -> NagLevel.STRESSED
            remaining <= 24 * HOUR_MS -> NagLevel.WORRIED
            remaining <= 72 * HOUR_MS -> NagLevel.CALM
            else -> NagLevel.DORMANT
        }
    }
}
