package com.yral.shared.iap.di

import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.providers.AndroidIAPProvider
import com.yral.shared.iap.providers.IAPProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

internal actual fun Scope.createIAPProvider(): IAPProvider =
    AndroidIAPProvider(
        context = androidContext(),
        appDispatchers = get(),
        preferences = get(),
        sessionManager = get(),
    )

internal actual fun wireWarningNotifier(
    manager: IAPManager,
    provider: IAPProvider,
) {
    if (provider is AndroidIAPProvider) {
        provider.setWarningNotifier { message ->
            manager.notifyWarning(message)
        }
    }
}
