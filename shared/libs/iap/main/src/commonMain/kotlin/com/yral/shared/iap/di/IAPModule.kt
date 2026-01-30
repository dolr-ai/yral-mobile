package com.yral.shared.iap.di

import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.providers.IAPProvider
import com.yral.shared.iap.providers.IAPProviderImpl
import com.yral.shared.iap.verification.PurchaseVerificationService
import io.ktor.client.HttpClient
import org.koin.dsl.module

val iapModule =
    module {
        single<PurchaseVerificationService> {
            PurchaseVerificationService(
                httpClient = get<HttpClient>(),
                json = get(),
            )
        }
        single<IAPProvider> {
            val coreProvider: com.yral.shared.iap.core.providers.IAPProvider = get()
            IAPProviderImpl(
                coreProvider = coreProvider,
                sessionManager = get<SessionManager>(),
                verificationService = get<PurchaseVerificationService>(),
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
