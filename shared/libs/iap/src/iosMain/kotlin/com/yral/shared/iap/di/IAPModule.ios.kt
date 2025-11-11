package com.yral.shared.iap.di

import com.yral.shared.iap.providers.IAPProvider
import com.yral.shared.iap.providers.IOSIAPProvider
import org.koin.core.scope.Scope

internal actual fun Scope.createIAPProvider(): IAPProvider = IOSIAPProvider()
