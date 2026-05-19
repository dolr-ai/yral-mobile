package com.yral.shared.iap.di

import com.yral.shared.core.di.BILLING_SERVER_BASE_URL
import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.account.PurchaseAccountResolver
import com.yral.shared.iap.account.createPurchaseAccountResolver
import com.yral.shared.iap.providers.IAPProvider
import com.yral.shared.iap.providers.IAPProviderImpl
import com.yral.shared.iap.verification.PurchaseVerificationService
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val iapModule =
    module {
        single<PurchaseAccountResolver> {
            createPurchaseAccountResolver(
                httpClient = get<HttpClient>(),
                json = get<Json>(),
                billingBaseUrl = get(BILLING_SERVER_BASE_URL),
            )
        }
        single<PurchaseVerificationService> {
            PurchaseVerificationService(
                httpClient = get<HttpClient>(),
                json = get(),
                billingBaseUrl = get(BILLING_SERVER_BASE_URL),
            )
        }
        single<IAPProvider> {
            val coreProvider: com.yral.shared.iap.core.providers.IAPProvider = get()
            IAPProviderImpl(
                coreProvider = coreProvider,
                sessionManager = get<SessionManager>(),
                verificationService = get<PurchaseVerificationService>(),
                purchaseAccountResolver = get<PurchaseAccountResolver>(),
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
