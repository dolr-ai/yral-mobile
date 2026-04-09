package com.yral.shared.data.data.models

import com.yral.shared.data.domain.models.DailyStreak
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyStreakDto(
    @SerialName("just_incremented") val justIncremented: Boolean,
    @SerialName("last_credited_at_epoch_ms") val lastCreditedAtEpochMs: Long,
    @SerialName("next_increment_eligible_at_epoch_ms") val nextIncrementEligibleAtEpochMs: Long,
    @SerialName("principal_id") val principalId: String,
    @SerialName("server_now_epoch_ms") val serverNowEpochMs: Long,
    @SerialName("streak_action") val streakAction: String,
    @SerialName("streak_count") val streakCount: Long,
    @SerialName("streak_expires_at_epoch_ms") val streakExpiresAtEpochMs: Long,
)

fun DailyStreakDto.toDomain() =
    DailyStreak(
        justIncremented = justIncremented,
        streakCount = streakCount,
        streakAction = streakAction,
        streakExpiresAtEpochMs = streakExpiresAtEpochMs,
        nextIncrementEligibleAtEpochMs = nextIncrementEligibleAtEpochMs,
        serverNowEpochMs = serverNowEpochMs,
    )
