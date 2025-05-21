package com.yral.shared.features.game.di

import com.yral.shared.features.game.data.GameRemoteDataSource
import com.yral.shared.features.game.data.IGameRemoteDataSource
import com.yral.shared.features.game.domain.GameRepository
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.GetGameRulesUseCase
import com.yral.shared.features.game.domain.IGameRepository
import com.yral.shared.features.game.viewmodel.GameViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val gameModule =
    module {
        singleOf(::GetGameIconsUseCase)
        singleOf(::GetGameRulesUseCase)
        viewModelOf(::GameViewModel)
        singleOf(::GameRepository) { bind<IGameRepository>() }
        singleOf(::GameRemoteDataSource) { bind<IGameRemoteDataSource>() }
    }
