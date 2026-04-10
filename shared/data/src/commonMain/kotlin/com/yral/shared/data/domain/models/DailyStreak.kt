package com.yral.shared.data.domain.models

data class DailyStreak(
    val justIncremented: Boolean,
    val streakCount: Long,
    val streakAction: String,
    val streakExpiresAtEpochMs: Long,
    val nextIncrementEligibleAtEpochMs: Long,
    val serverNowEpochMs: Long,
)
