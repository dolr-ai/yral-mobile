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
import com.yral.shared.features.feed.domain.useCases.GetGlobalCacheFeedUseCase
import com.yral.shared.features.feed.domain.useCases.GetInitialFeedUseCase
import com.yral.shared.features.feed.domain.useCases.GetTournamentFeedUseCase
import com.yral.shared.features.feed.domain.useCases.LoadCachedFeedDetailsUseCase
import com.yral.shared.features.feed.domain.useCases.SaveFeedDetailsCacheUseCase
import com.yral.shared.features.feed.viewmodel.FeedContext
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.RequiredUseCases
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val feedModule =
    module {
        factoryOf(::GetInitialFeedUseCase)
        factoryOf(::GetGlobalCacheFeedUseCase)
        factoryOf(::FetchMoreFeedUseCase)
        factoryOf(::FetchFeedDetailsUseCase)
        factoryOf(::FetchFeedDetailsWithCreatorInfoUseCase)
        factoryOf(::GetAIFeedUseCase)
        factoryOf(::GetTournamentFeedUseCase)
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
        viewModel { parameters ->
            val feedContext = parameters.getOrNull<FeedContext>() ?: FeedContext.Default
            FeedViewModel(
                appDispatchers = get(),
                sessionManager = get(),
                requiredUseCases = get(),
                crashlyticsManager = get(),
                feedTelemetry = get(),
                shareService = get(),
                urlBuilder = get(),
                linkGenerator = get(),
                flagManager = get(),
                preferences = get(),
                feedContext = feedContext,
            )
        }
        factoryOf(::FeedRepository) { bind<IFeedRepository>() }
        factoryOf(::FeedRemoteDataSource) { bind<IFeedDataSource>() }
        factoryOf(::RequiredUseCases)
        factoryOf(::FeedTelemetry)
    }
