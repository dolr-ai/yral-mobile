package com.yral.shared.features.game.di

import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.viewmodel.GameViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val gameModule =
    module {
        singleOf(::GetGameIconsUseCase)
        viewModelOf(::GameViewModel)
    }
