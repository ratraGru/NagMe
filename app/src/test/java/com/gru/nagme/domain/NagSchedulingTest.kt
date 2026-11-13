package com.gru.nagme.domain

import com.gru.nagme.data.TaskEntity
import com.gru.nagme.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NagSchedulingTest {
    private val hour = 3_600_000L
    private val minute = 60_000L
    private val now = 1_000_000_000_000L
    private val deadline = now + 100 * hour

    private fun task(
        deadlineEpoch: Long = deadline,
        last: Int = 0,
        status: Int = TaskStatus.ACTIVE,
        nag: Boolean = true,
        snoozeUntil: Long = 0L
    ) = TaskEntity(
        id = 1,
        title = "Tugas",
        description = null,
        deadlineEpoch = deadlineEpoch,
        createdAt = 0,
        updatedAt = 0,
        status = status,
        nagEnabled = nag,
        snoozeUntilEpoch = snoozeUntil,
        lastTriggeredLevel = last
    )

    @Test fun triggerEpochMatchesResolverForEveryLevel() {
        NagLevel.entries.filter { it != NagLevel.DORMANT }.forEach { level ->
            val at = NagScheduling.triggerEpoch(deadline, level)
            assertEquals(level, NagLevelResolver.resolve(deadline, at))
            if (level != NagLevel.CALM) {
                // 1 ms sebelum batas, level masih lebih rendah
                assertEquals(
                    NagLevel.entries[level.ordinal - 1],
                    NagLevelResolver.resolve(deadline, at - 1)
                )
            }
        }
    }

    @Test fun notDueWhileStillDormant() {
        assertNull(NagScheduling.dueLevel(task(last = 0), now))
    }

    @Test fun dueWhenLevelEscalates() {
        val at = NagScheduling.triggerEpoch(deadline, NagLevel.WORRIED)
        assertEquals(NagLevel.WORRIED, NagScheduling.dueLevel(task(last = 1), at))
    }

    @Test fun notDueTwiceForTheSameLevel() {
        val at = NagScheduling.triggerEpoch(deadline, NagLevel.WORRIED)
        assertNull(NagScheduling.dueLevel(task(last = 2), at))
    }

    @Test fun lateAlarmStillFiresCurrentLevelOnce() {
        // Alarm telat: sudah di L4 padahal terakhir baru L2 => muncul L4 (bukan L3 lalu L4)
        val at = NagScheduling.triggerEpoch(deadline, NagLevel.PANIC) + minute
        assertEquals(NagLevel.PANIC, NagScheduling.dueLevel(task(last = 2), at))
    }

    @Test fun overdueFiresExactlyAtDeadline() {
        assertEquals(NagLevel.OVERDUE, NagScheduling.dueLevel(task(last = 5), deadline))
        assertNull(NagScheduling.dueLevel(task(last = 6), deadline + hour))
    }

    @Test fun completedOrDisabledTasksNeverFire() {
        assertNull(NagScheduling.dueLevel(task(last = 0, status = TaskStatus.DONE), deadline))
        assertNull(NagScheduling.dueLevel(task(last = 0, nag = false), deadline))
        assertNull(NagScheduling.nextTriggerEpoch(task(status = TaskStatus.DONE), now))
        assertNull(NagScheduling.nextTriggerEpoch(task(nag = false), now))
    }

    @Test fun snoozeSuppressesThenFiresAgainAtCurrentLevel() {
        val snoozeEnd = now + 10 * minute
        val t = task(last = 3, snoozeUntil = snoozeEnd)
        assertNull(NagScheduling.dueLevel(t, now + 5 * minute))
        val d = deadline - 5 * hour // L4
        val t2 = task(deadlineEpoch = d, last = 3, snoozeUntil = d - 5 * hour + 1)
        assertEquals(NagLevel.PANIC, NagScheduling.dueLevel(t2, d - 5 * hour + 2))
    }

    @Test fun nextTriggerIsTheNextHigherLevel() {
        assertEquals(
            NagScheduling.triggerEpoch(deadline, NagLevel.CALM),
            NagScheduling.nextTriggerEpoch(task(last = 0), now)
        )
        assertEquals(
            NagScheduling.triggerEpoch(deadline, NagLevel.PANIC),
            NagScheduling.nextTriggerEpoch(task(last = 3), now)
        )
        assertEquals(deadline, NagScheduling.nextTriggerEpoch(task(last = 5), now))
        assertNull(NagScheduling.nextTriggerEpoch(task(last = 6), now))
    }

    @Test fun nextTriggerPrefersSnoozeEnd() {
        val end = now + 10 * minute
        assertEquals(end, NagScheduling.nextTriggerEpoch(task(last = 2, snoozeUntil = end), now))
    }
}
