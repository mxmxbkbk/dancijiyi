package com.dancijiyi.domain.ebbinghaus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class EbbinghausUtilTest {

    private val baseTime = 1_700_000_000_000L

    @Test
    fun intervals_matchConfiguredLevels() {
        assertEquals(TimeUnit.MINUTES.toMillis(5), EbbinghausUtil.getIntervalMillis(0))
        assertEquals(TimeUnit.MINUTES.toMillis(30), EbbinghausUtil.getIntervalMillis(1))
        assertEquals(TimeUnit.HOURS.toMillis(12), EbbinghausUtil.getIntervalMillis(2))
        assertEquals(TimeUnit.DAYS.toMillis(1), EbbinghausUtil.getIntervalMillis(3))
        assertEquals(TimeUnit.DAYS.toMillis(2), EbbinghausUtil.getIntervalMillis(4))
        assertEquals(TimeUnit.DAYS.toMillis(4), EbbinghausUtil.getIntervalMillis(5))
        assertEquals(TimeUnit.DAYS.toMillis(7), EbbinghausUtil.getIntervalMillis(6))
    }

    @Test
    fun nextReviewTime_usesCurrentLevelInterval() {
        assertEquals(baseTime + TimeUnit.MINUTES.toMillis(5), EbbinghausUtil.calculateNextReviewTime(0, baseTime))
        assertEquals(baseTime + TimeUnit.MINUTES.toMillis(30), EbbinghausUtil.calculateNextReviewTime(1, baseTime))
        assertEquals(baseTime + TimeUnit.HOURS.toMillis(12), EbbinghausUtil.calculateNextReviewTime(2, baseTime))
        assertEquals(baseTime + TimeUnit.DAYS.toMillis(1), EbbinghausUtil.calculateNextReviewTime(3, baseTime))
        assertEquals(baseTime + TimeUnit.DAYS.toMillis(2), EbbinghausUtil.calculateNextReviewTime(4, baseTime))
        assertEquals(baseTime + TimeUnit.DAYS.toMillis(4), EbbinghausUtil.calculateNextReviewTime(5, baseTime))
    }

    @Test
    fun maxLevel_hasNoNextReview() {
        assertNull(EbbinghausUtil.calculateNextReviewTime(6, baseTime))
        assertTrue(EbbinghausUtil.isMastered(6))
        assertFalse(EbbinghausUtil.isMastered(5))
    }

    @Test
    fun correctReview_upgradesAndStopsAtMaxLevel() {
        assertEquals(1, EbbinghausUtil.onReviewCorrect(0))
        assertEquals(6, EbbinghausUtil.onReviewCorrect(5))
        assertEquals(6, EbbinghausUtil.onReviewCorrect(6))
    }

    @Test
    fun wrongReview_resetsToZero() {
        assertEquals(0, EbbinghausUtil.onReviewWrong())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidLevel_isRejected() {
        EbbinghausUtil.getIntervalMillis(7)
    }
}
