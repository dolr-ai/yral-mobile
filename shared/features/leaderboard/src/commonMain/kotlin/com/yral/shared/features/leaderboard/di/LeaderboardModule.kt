package com.yral.shared.features.leaderboard.di

import com.yral.shared.features.leaderboard.analytics.LeaderBoardTelemetry
import com.yral.shared.features.leaderboard.data.ILeaderboardRemoteDataSource
import com.yral.shared.features.leaderboard.data.LeaderboardRemoteDataSource
import com.yral.shared.features.leaderboard.data.LeaderboardRepository
import com.yral.shared.features.leaderboard.domain.GetLeaderboardHistoryUseCase
import com.yral.shared.features.leaderboard.domain.GetLeaderboardUseCase
import com.yral.shared.features.leaderboard.domain.ILeaderboardRepository
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel
import com.yral.shared.features.leaderboard.viewmodel.LeaderboardHistoryViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val leaderboardModule =
    module {
        factoryOf(::GetLeaderboardUseCase)
        factoryOf(::GetLeaderboardHistoryUseCase)
        viewModelOf(::LeaderBoardViewModel)
        viewModelOf(::LeaderboardHistoryViewModel)
        factoryOf(::LeaderboardRepository) { bind<ILeaderboardRepository>() }
        factoryOf(::LeaderboardRemoteDataSource) { bind<ILeaderboardRemoteDataSource>() }
        factoryOf(::LeaderBoardTelemetry)
    }
