package com.yral.shared.iap.core.di

import com.yral.shared.iap.core.providers.IAPProvider
import com.yral.shared.iap.core.providers.IOSIAPProvider
import org.koin.core.scope.Scope

internal actual fun Scope.createIAPProvider(): IAPProvider = IOSIAPProvider()
