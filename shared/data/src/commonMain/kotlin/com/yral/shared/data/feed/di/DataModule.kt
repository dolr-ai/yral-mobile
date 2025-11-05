package com.yral.shared.data.feed.di

import com.yral.shared.data.feed.data.CommonApisDataSource
import com.yral.shared.data.feed.data.CommonApisImpl
import com.yral.shared.data.feed.data.CommonApisRemoteDataSource
import com.yral.shared.data.feed.domain.CommonApis
import com.yral.shared.data.feed.domain.useCases.GetVideoViewsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val commonDataModule =
    module {
        factoryOf(::CommonApisImpl).bind<CommonApis>()
        factoryOf(::CommonApisRemoteDataSource).bind<CommonApisDataSource>()
        factoryOf(::GetVideoViewsUseCase)
    }
