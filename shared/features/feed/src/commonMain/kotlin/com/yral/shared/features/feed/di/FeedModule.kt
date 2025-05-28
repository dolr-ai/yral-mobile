package com.yral.shared.features.feed.di

import com.yral.shared.features.feed.data.FeedRemoteDataSource
import com.yral.shared.features.feed.data.FeedRepository
import com.yral.shared.features.feed.data.IFeedRemoteDataSource
import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.features.feed.useCases.ReportVideoUseCase
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.RequiredUseCases
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val feedModule =
    module {
        factoryOf(::GetInitialFeedUseCase)
        factoryOf(::FetchMoreFeedUseCase)
        factoryOf(::FetchFeedDetailsUseCase)
        factoryOf(::ReportVideoUseCase)
        factoryOf(::RequiredUseCases)
        viewModelOf(::FeedViewModel)
        factoryOf(::FeedRepository) { bind<IFeedRepository>() }
        factoryOf(::FeedRemoteDataSource) { bind<IFeedRemoteDataSource>() }
    }
