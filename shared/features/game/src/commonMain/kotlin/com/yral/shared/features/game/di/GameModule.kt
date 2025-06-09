package com.yral.shared.features.game.di

import com.yral.shared.features.game.data.GameRemoteDataSource
import com.yral.shared.features.game.data.GameRepository
import com.yral.shared.features.game.data.IGameRemoteDataSource
import com.yral.shared.features.game.domain.CastVoteUseCase
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
        factory { GetGameIconsUseCase(get(), get(), get(named("GameConfig"))) }
        factory { GetGameRulesUseCase(get(), get(), get(named("AboutGame"))) }
        factory { GetLeaderboardUseCase(get(), get(), get(named("LeaderBoard"))) }
        viewModelOf(::GameViewModel)
        viewModelOf(::LeaderBoardViewModel)
        factoryOf(::GameRepository) { bind<IGameRepository>() }
        factoryOf(::GameRemoteDataSource) { bind<IGameRemoteDataSource>() }
    }
