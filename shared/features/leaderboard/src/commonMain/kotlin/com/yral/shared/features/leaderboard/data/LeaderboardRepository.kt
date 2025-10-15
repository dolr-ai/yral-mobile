package com.yral.shared.features.leaderboard.data

import com.github.michaelbull.result.Result
import com.yral.shared.features.leaderboard.data.models.toLeaderboardData
import com.yral.shared.features.leaderboard.data.models.toLeaderboardHistory
import com.yral.shared.features.leaderboard.data.models.toLeaderboardRankData
import com.yral.shared.features.leaderboard.domain.ILeaderboardRepository
import com.yral.shared.features.leaderboard.domain.models.GetLeaderboardRequest
import com.yral.shared.features.leaderboard.domain.models.LeaderboardDailyRank
import com.yral.shared.features.leaderboard.domain.models.LeaderboardDailyRankRequest
import com.yral.shared.features.leaderboard.domain.models.LeaderboardData
import com.yral.shared.features.leaderboard.domain.models.LeaderboardError
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistory
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryError
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryRequest
import com.yral.shared.features.leaderboard.domain.models.toDto

class LeaderboardRepository(
    private val leaderboardRemoteDataSource: ILeaderboardRemoteDataSource,
) : ILeaderboardRepository {
    override suspend fun getLeaderboard(
        idToken: String,
        request: GetLeaderboardRequest,
    ): Result<LeaderboardData, LeaderboardError> =
        leaderboardRemoteDataSource
            .getLeaderboard(idToken, request.toDto())
            .toLeaderboardData()

    override suspend fun getLeaderboardHistory(
        idToken: String,
        request: LeaderboardHistoryRequest,
    ): Result<LeaderboardHistory, LeaderboardHistoryError> =
        leaderboardRemoteDataSource
            .getLeaderboardHistory(idToken, request.toDto())
            .toLeaderboardHistory()

    override suspend fun getLeaderboardRankForToday(
        idToken: String,
        request: LeaderboardDailyRankRequest,
    ): Result<LeaderboardDailyRank, LeaderboardError> =
        leaderboardRemoteDataSource
            .getLeaderboardRankForToday(idToken, request.toDto())
            .toLeaderboardRankData()
}
