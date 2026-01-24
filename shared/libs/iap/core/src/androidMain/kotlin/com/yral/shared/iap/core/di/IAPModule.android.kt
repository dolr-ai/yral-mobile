package com.yral.shared.iap.core.di

import com.yral.shared.iap.core.providers.AndroidIAPProvider
import com.yral.shared.iap.core.providers.IAPProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

internal actual fun Scope.createIAPProvider(): IAPProvider =
    AndroidIAPProvider(
        context = androidContext(),
        appDispatchers = get(),
    )
