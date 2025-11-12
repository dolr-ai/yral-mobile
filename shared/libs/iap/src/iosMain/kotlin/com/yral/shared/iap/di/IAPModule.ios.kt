package com.yral.shared.iap.di

import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.providers.IAPProvider
import com.yral.shared.iap.providers.IOSIAPProvider
import org.koin.core.scope.Scope

internal actual fun Scope.createIAPProvider(): IAPProvider = IOSIAPProvider()

internal actual fun wireWarningNotifier(
    manager: IAPManager,
    provider: IAPProvider,
) {
    if (provider is IOSIAPProvider) {
        provider.setWarningNotifier { message ->
            manager.notifyWarning(message)
        }
    }
}
