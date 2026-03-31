package com.yral.shared.data.di

import com.yral.shared.data.data.CommonApisDataSource
import com.yral.shared.data.data.CommonApisImpl
import com.yral.shared.data.data.CommonApisRemoteDataSource
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.useCases.GetVideoViewsUseCase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.storage.storage
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val commonDataModule =
    module {
        factory { Firebase.storage(Firebase.app) }
        factoryOf(::CommonApisImpl).bind<CommonApis>()
        factoryOf(::CommonApisRemoteDataSource).bind<CommonApisDataSource>()
        factoryOf(::GetVideoViewsUseCase)
    }
