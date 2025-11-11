package com.yral.shared.iap.di

import com.yral.shared.iap.providers.AndroidIAPProvider
import com.yral.shared.iap.providers.IAPProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

internal actual fun Scope.createIAPProvider(): IAPProvider =
    AndroidIAPProvider(
        context = androidContext(),
        appDispatchers = get(),
    )
