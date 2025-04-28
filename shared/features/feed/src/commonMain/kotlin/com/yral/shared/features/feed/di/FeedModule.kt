package com.yral.shared.features.feed.di

import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val feedModule =
    module {
        factoryOf(::GetInitialFeedUseCase)
        factoryOf(::FetchMoreFeedUseCase)
        factoryOf(::FetchFeedDetailsUseCase)
    }
