package com.yral.shared.iap.di

import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.providers.IAPProvider
import com.yral.shared.iap.providers.IAPProviderImpl
import org.koin.dsl.module

val iapModule =
    module {
        single<IAPProvider> {
            val coreProvider: com.yral.shared.iap.core.providers.IAPProvider = get()
            IAPProviderImpl(
                coreProvider = coreProvider,
                preferences = get(),
                sessionManager = get<SessionManager>(),
            )
        }
        single<IAPManager> {
            val provider: IAPProvider = get()
            val manager = IAPManager(provider, get())
            if (provider is IAPProviderImpl) {
                provider.setWarningNotifier { message ->
                    manager.notifyWarning(message)
                }
            }
            manager
        }
    }
