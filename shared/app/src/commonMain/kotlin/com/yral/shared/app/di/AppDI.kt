package com.yral.shared.app.di

import com.yral.shared.app.config.AppUseCaseFailureListener
import com.yral.shared.app.config.NBRFailureListener
import com.yral.shared.libs.arch.data.NetworkBoundResource
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

expect fun initKoin(appDeclaration: KoinAppDeclaration)

internal val dispatchersModule = module { single { AppDispatchers() } }

internal val archModule =
    module {
        singleOf(::NBRFailureListener) bind NetworkBoundResource.OnFailureListener::class
        singleOf(::AppUseCaseFailureListener) bind UseCaseFailureListener::class
    }
