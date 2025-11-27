package com.yral.shared.features.feed.di

import com.yral.shared.features.feed.analytics.FeedTelemetry
import com.yral.shared.features.feed.data.FeedRemoteDataSource
import com.yral.shared.features.feed.data.FeedRepository
import com.yral.shared.features.feed.data.IFeedDataSource
import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.useCases.CheckVideoVoteUseCase
import com.yral.shared.features.feed.domain.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.domain.useCases.FetchFeedDetailsWithCreatorInfoUseCase
import com.yral.shared.features.feed.domain.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.domain.useCases.GetAIFeedUseCase
import com.yral.shared.features.feed.domain.useCases.GetInitialFeedUseCase
import com.yral.shared.features.feed.domain.useCases.LoadCachedFeedDetailsUseCase
import com.yral.shared.features.feed.domain.useCases.SaveFeedDetailsCacheUseCase
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.RequiredUseCases
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val feedModule =
    module {
        factoryOf(::GetInitialFeedUseCase)
        factoryOf(::FetchMoreFeedUseCase)
        factoryOf(::FetchFeedDetailsUseCase)
        factoryOf(::FetchFeedDetailsWithCreatorInfoUseCase)
        factoryOf(::GetAIFeedUseCase)
        factoryOf(::CheckVideoVoteUseCase)
        factory {
            LoadCachedFeedDetailsUseCase(
                preferences = get(named("FeedCachePreferences")),
                json = get(),
                appDispatchers = get(),
                useCaseFailureListener = get(),
            )
        }
        factory {
            SaveFeedDetailsCacheUseCase(
                preferences = get(named("FeedCachePreferences")),
                json = get(),
                appDispatchers = get(),
                useCaseFailureListener = get(),
            )
        }
        viewModelOf(::FeedViewModel)
        factoryOf(::FeedRepository) { bind<IFeedRepository>() }
        factoryOf(::FeedRemoteDataSource) { bind<IFeedDataSource>() }
        factoryOf(::RequiredUseCases)
        factoryOf(::FeedTelemetry)
    }
