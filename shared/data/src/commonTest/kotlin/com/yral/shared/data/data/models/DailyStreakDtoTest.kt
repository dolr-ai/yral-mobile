package com.yral.shared.data.data.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyStreakDtoTest {
    @Test
    fun toDomainMapsStreakCountCorrectly() {
        val dto = buildDto(streakCount = 5L)
        assertEquals(5L, dto.toDomain().streakCount)
    }

    @Test
    fun toDomainMapsStreakExpiresAtCorrectly() {
        val dto = buildDto(streakExpiresAtEpochMs = 9_007_199_254_740_991L)
        assertEquals(9_007_199_254_740_991L, dto.toDomain().streakExpiresAtEpochMs)
    }

    @Test
    fun toDomainMapsJustIncrementedTrueCorrectly() {
        val dto = buildDto(justIncremented = true)
        assertTrue(dto.toDomain().justIncremented)
    }

    @Test
    fun toDomainMapsJustIncrementedFalseCorrectly() {
        val dto = buildDto(justIncremented = false)
        assertFalse(dto.toDomain().justIncremented)
    }

    @Test
    fun toDomainMapsStreakActionCorrectly() {
        val dto = buildDto(streakAction = "daily_login")
        assertEquals("daily_login", dto.toDomain().streakAction)
    }

    @Test
    fun toDomainMapsNextIncrementEligibleAtCorrectly() {
        val dto = buildDto(nextIncrementEligibleAtEpochMs = 2_000_000L)
        assertEquals(2_000_000L, dto.toDomain().nextIncrementEligibleAtEpochMs)
    }

    @Test
    fun toDomainMapsServerNowCorrectly() {
        val dto = buildDto(serverNowEpochMs = 1_000_000L)
        assertEquals(1_000_000L, dto.toDomain().serverNowEpochMs)
    }

    @Test
    fun toDomainHandlesZeroStreakCount() {
        val dto = buildDto(streakCount = 0L)
        assertEquals(0L, dto.toDomain().streakCount)
    }
}

private fun buildDto(
    justIncremented: Boolean = true,
    streakCount: Long = 1L,
    streakAction: String = "daily_login",
    streakExpiresAtEpochMs: Long = 1_000_000L,
    nextIncrementEligibleAtEpochMs: Long = 2_000_000L,
    serverNowEpochMs: Long = 500_000L,
) = DailyStreakDto(
    justIncremented = justIncremented,
    lastCreditedAtEpochMs = 0L,
    nextIncrementEligibleAtEpochMs = nextIncrementEligibleAtEpochMs,
    principalId = "test-principal",
    serverNowEpochMs = serverNowEpochMs,
    streakAction = streakAction,
    streakCount = streakCount,
    streakExpiresAtEpochMs = streakExpiresAtEpochMs,
)
