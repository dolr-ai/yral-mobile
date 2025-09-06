package com.yral.shared.features.game.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.game.domain.models.LeaderboardData
import com.yral.shared.features.game.domain.models.LeaderboardError
import com.yral.shared.features.game.domain.models.LeaderboardErrorCodes
import com.yral.shared.features.game.domain.models.toLeaderboardItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class LeaderboardResponseDto {
    @Serializable
    data class Success(
        @SerialName("user_row")
        val userRow: LeaderboardRowDto? = null,
        @SerialName("top_rows")
        val topRows: List<LeaderboardRowDto>,
        @SerialName("time_left_ms")
        val timeLeftMs: Long?,
    ) : LeaderboardResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: LeaderboardErrorDto,
    ) : LeaderboardResponseDto()
}

@Serializable
data class LeaderboardRowDto(
    @SerialName("principal_id")
    val principalId: String,
    @SerialName("wins")
    val wins: Long,
    @SerialName("position")
    val position: Int,
)

@Serializable
data class LeaderboardErrorDto(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)

fun LeaderboardResponseDto.toLeaderboardData(): Result<LeaderboardData, LeaderboardError> =
    when (this) {
        is LeaderboardResponseDto.Success -> {
            Ok(
                LeaderboardData(
                    userRow = userRow?.toLeaderboardItem(),
                    topRows = topRows.map { it.toLeaderboardItem() },
                    timeLeftMs = timeLeftMs,
                ),
            )
        }
        is LeaderboardResponseDto.Error -> {
            Err(
                LeaderboardError(
                    code = LeaderboardErrorCodes.valueOf(error.code),
                    message = error.message,
                ),
            )
        }
    }
