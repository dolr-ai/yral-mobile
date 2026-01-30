package com.yral.shared.iap.core.di

import com.yral.shared.iap.core.IAPManager
import com.yral.shared.iap.core.providers.IAPProvider
import org.koin.core.scope.Scope
import org.koin.dsl.module

internal expect fun Scope.createIAPProvider(): IAPProvider

val iapCoreModule =
    module {
        single<IAPProvider> { createIAPProvider() }
        single<IAPManager> { IAPManager(get()) }
    }
