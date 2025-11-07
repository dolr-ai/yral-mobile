package com.yral.shared.data.di

import com.yral.shared.data.data.CommonApisDataSource
import com.yral.shared.data.data.CommonApisImpl
import com.yral.shared.data.data.CommonApisRemoteDataSource
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.useCases.GetVideoViewsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val commonDataModule =
    module {
        factoryOf(::CommonApisImpl).bind<CommonApis>()
        factoryOf(::CommonApisRemoteDataSource).bind<CommonApisDataSource>()
        factoryOf(::GetVideoViewsUseCase)
    }
