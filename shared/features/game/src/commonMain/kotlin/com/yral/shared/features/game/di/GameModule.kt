package com.yral.shared.features.game.di

import com.yral.shared.features.game.analytics.GameTelemetry
import com.yral.shared.features.game.analytics.LeaderBoardTelemetry
import com.yral.shared.features.game.data.GameRemoteDataSource
import com.yral.shared.features.game.data.GameRepository
import com.yral.shared.features.game.data.IGameRemoteDataSource
import com.yral.shared.features.game.domain.CastVoteUseCase
import com.yral.shared.features.game.domain.GetBalanceUseCase
import com.yral.shared.features.game.domain.GetCurrentUserInfoUseCase
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.GetGameRulesUseCase
import com.yral.shared.features.game.domain.GetLeaderboardUseCase
import com.yral.shared.features.game.domain.IGameRepository
import com.yral.shared.features.game.viewmodel.GameViewModel
import com.yral.shared.features.game.viewmodel.LeaderBoardViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val gameModule =
    module {
        factoryOf(::CastVoteUseCase)
        factoryOf(::GetBalanceUseCase)
        factory { GetGameIconsUseCase(get(), get(), get(named("GameConfig")), get()) }
        factory { GetGameRulesUseCase(get(), get(), get(named("AboutGame")), get()) }
        factory { GetLeaderboardUseCase(get(), get(), get(named("LeaderBoard"))) }
        factory { GetCurrentUserInfoUseCase(get(), get(), get(named("LeaderBoard")), get()) }
        viewModelOf(::GameViewModel)
        viewModelOf(::LeaderBoardViewModel)
        factoryOf(::GameRepository) { bind<IGameRepository>() }
        factoryOf(::GameRemoteDataSource) { bind<IGameRemoteDataSource>() }
        factoryOf(::LeaderBoardTelemetry)
        factoryOf(::GameTelemetry)
    }
