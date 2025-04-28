package com.yral.android.di

import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.auth.DefaultAuthClient
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val authModule =
    module {
        singleOf(::DefaultAuthClient) { bind<AuthClient>() }
    }

internal val feedModule =
    module {
        factoryOf(::GetInitialFeedUseCase)
        factoryOf(::FetchMoreFeedUseCase)
        factoryOf(::FetchFeedDetailsUseCase)
    }
