package com.yral.shared.features.tournament.domain.model

data class DailySessionResult(
    val remainingTimeMs: Long,
    val diamonds: Int,
    val wins: Int,
    val losses: Int,
    val position: Int,
    val timeSpentMs: Long,
)
