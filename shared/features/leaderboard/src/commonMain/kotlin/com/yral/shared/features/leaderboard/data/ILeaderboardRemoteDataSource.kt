package com.yral.shared.features.leaderboard.data

import com.yral.shared.features.leaderboard.data.models.GetLeaderboardRequestDto
import com.yral.shared.features.leaderboard.data.models.LeaderboardHistoryRequestDto
import com.yral.shared.features.leaderboard.data.models.LeaderboardHistoryResponseDto
import com.yral.shared.features.leaderboard.data.models.LeaderboardResponseDto

interface ILeaderboardRemoteDataSource {
    suspend fun getLeaderboard(
        idToken: String,
        request: GetLeaderboardRequestDto,
    ): LeaderboardResponseDto

    suspend fun getLeaderboardHistory(
        idToken: String,
        request: LeaderboardHistoryRequestDto,
    ): LeaderboardHistoryResponseDto
}
