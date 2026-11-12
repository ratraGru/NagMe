package com.gru.nagme.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NagLevelResolverTest {
    private val hour = 3_600_000L
    private val now = 1_000_000_000_000L

    private fun levelAt(remainingMs: Long) = NagLevelResolver.resolve(now + remainingMs, now)

    @Test fun moreThan72HoursIsDormant() {
        assertEquals(NagLevel.DORMANT, levelAt(72 * hour + 1))
        assertEquals(NagLevel.DORMANT, levelAt(200 * hour))
    }

    @Test fun boundariesBelongToTheMoreUrgentLevel() {
        assertEquals(NagLevel.CALM, levelAt(72 * hour))
        assertEquals(NagLevel.WORRIED, levelAt(24 * hour))
        assertEquals(NagLevel.STRESSED, levelAt(12 * hour))
        assertEquals(NagLevel.PANIC, levelAt(6 * hour))
        assertEquals(NagLevel.CRITICAL, levelAt(3 * hour))
    }

    @Test fun justOverABoundaryIsTheLessUrgentLevel() {
        assertEquals(NagLevel.CALM, levelAt(24 * hour + 1))
        assertEquals(NagLevel.WORRIED, levelAt(12 * hour + 1))
        assertEquals(NagLevel.STRESSED, levelAt(6 * hour + 1))
        assertEquals(NagLevel.PANIC, levelAt(3 * hour + 1))
    }

    @Test fun zeroOrNegativeRemainingIsOverdue() {
        assertEquals(NagLevel.OVERDUE, levelAt(0))
        assertEquals(NagLevel.OVERDUE, levelAt(-5 * hour))
    }

    @Test fun levelValuesMatchTheDocument() {
        assertEquals(0, NagLevel.DORMANT.value)
        assertEquals(5, NagLevel.CRITICAL.value)
        assertEquals(6, NagLevel.OVERDUE.value)
    }
}
