package com.yral.shared.iap.di

import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.providers.IAPProvider
import org.koin.core.scope.Scope
import org.koin.dsl.module

/**
 * Internal function to create platform-specific IAPProvider.
 * Used by DI module only.
 */
internal expect fun Scope.createIAPProvider(): IAPProvider

/**
 * Koin module for IAP functionality.
 * Register this module in your Koin configuration.
 */
val iapModule =
    module {
        single<IAPProvider> { createIAPProvider() }
        single<IAPManager> {
            val manager = IAPManager(get())
            wireWarningNotifier(manager, get())
            manager
        }
    }

internal expect fun wireWarningNotifier(
    manager: IAPManager,
    provider: IAPProvider,
)
