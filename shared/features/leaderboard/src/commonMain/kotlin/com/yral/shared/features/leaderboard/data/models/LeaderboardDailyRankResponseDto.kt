package com.yral.shared.features.leaderboard.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.leaderboard.domain.models.LeaderboardDailyRank
import com.yral.shared.features.leaderboard.domain.models.LeaderboardError
import com.yral.shared.features.leaderboard.domain.models.LeaderboardErrorCodes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class LeaderboardDailyRankResponseDto {
    @Serializable
    data class Success(
        @SerialName("position")
        val position: Long,
    ) : LeaderboardDailyRankResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: LeaderboardErrorDto,
    ) : LeaderboardDailyRankResponseDto()
}

fun LeaderboardDailyRankResponseDto.toLeaderboardRankData(): Result<LeaderboardDailyRank, LeaderboardError> =
    when (this) {
        is LeaderboardDailyRankResponseDto.Error ->
            Err(
                LeaderboardError(
                    code = LeaderboardErrorCodes.valueOf(error.code),
                    message = error.message,
                ),
            )
        is LeaderboardDailyRankResponseDto.Success -> Ok(LeaderboardDailyRank(position))
    }
