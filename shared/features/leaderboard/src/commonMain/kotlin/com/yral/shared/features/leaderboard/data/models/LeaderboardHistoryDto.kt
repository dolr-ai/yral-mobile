package com.yral.shared.features.leaderboard.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.leaderboard.domain.models.LeaderboardErrorCodes
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistory
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryDay
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryError
import com.yral.shared.features.leaderboard.domain.models.toLeaderboardItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardHistoryDayDto(
    @SerialName("date")
    val date: String,
    @SerialName("top_rows")
    val topRows: List<LeaderboardRowDto>,
    @SerialName("user_row")
    val userRow: LeaderboardRowDto? = null,
)

@Serializable
sealed class LeaderboardHistoryResponseDto {
    @Serializable
    data class Success(
        @SerialName("days")
        val days: List<LeaderboardHistoryDayDto>,
    ) : LeaderboardHistoryResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: LeaderboardErrorDto,
    ) : LeaderboardHistoryResponseDto()
}

fun LeaderboardHistoryResponseDto.toLeaderboardHistory(): Result<LeaderboardHistory, LeaderboardHistoryError> =
    when (this) {
        is LeaderboardHistoryResponseDto.Success ->
            Ok(
                days.map { day ->
                    LeaderboardHistoryDay(
                        date = day.date,
                        topRows = day.topRows.map { it.toLeaderboardItem() },
                        userRow = day.userRow?.toLeaderboardItem(),
                    )
                },
            )
        is LeaderboardHistoryResponseDto.Error ->
            Err(
                LeaderboardHistoryError(
                    code = LeaderboardErrorCodes.valueOf(error.code),
                    message = error.message,
                ),
            )
    }
