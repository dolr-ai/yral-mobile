package com.yral.shared.features.leaderboard.domain

import com.github.michaelbull.result.Result
import com.yral.shared.features.leaderboard.domain.models.GetLeaderboardRequest
import com.yral.shared.features.leaderboard.domain.models.LeaderboardDailyRank
import com.yral.shared.features.leaderboard.domain.models.LeaderboardDailyRankRequest
import com.yral.shared.features.leaderboard.domain.models.LeaderboardData
import com.yral.shared.features.leaderboard.domain.models.LeaderboardError
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistory
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryError
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryRequest

interface ILeaderboardRepository {
    suspend fun getLeaderboard(
        idToken: String,
        request: GetLeaderboardRequest,
    ): Result<LeaderboardData, LeaderboardError>

    suspend fun getLeaderboardHistory(
        idToken: String,
        request: LeaderboardHistoryRequest,
    ): Result<LeaderboardHistory, LeaderboardHistoryError>

    suspend fun getLeaderboardRankForToday(
        idToken: String,
        request: LeaderboardDailyRankRequest,
    ): Result<LeaderboardDailyRank, LeaderboardError>
}
