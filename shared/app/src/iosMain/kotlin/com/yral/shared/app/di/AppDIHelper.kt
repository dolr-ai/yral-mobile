package com.yral.shared.app.di

import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.iap.core.providers.AppleStoreKitBridge
import com.yral.shared.libs.routing.deeplink.engine.RoutingService
import com.yral.shared.preferences.stores.AffiliateAttributionStore
import com.yral.shared.preferences.stores.UtmAttributionStore
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.dsl.module

class AppDIHelper : KoinComponent {
    fun getFeatureFlagManager(): FeatureFlagManager = get()
    fun getRoutingService(): RoutingService = get()
    fun getAffiliateAttributionStore(): AffiliateAttributionStore = get()
    fun getUtmAttributionStore(): UtmAttributionStore = get()
}

interface ExternalDependencyProvider {
    val appleStoreKitBridge: AppleStoreKitBridge?
        get() = null
}

@Suppress("UnusedParameter")
fun KoinApplication.installExternalDependencyModule(provider: ExternalDependencyProvider) {
    provider.appleStoreKitBridge?.let { bridge ->
        modules(
            module {
                single<AppleStoreKitBridge> { bridge }
            },
        )
    }
}
