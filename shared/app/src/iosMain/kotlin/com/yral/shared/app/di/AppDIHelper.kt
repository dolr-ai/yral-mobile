package com.yral.shared.app.di

import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.analytics.providers.onesignal.OneSignalKMP
import com.yral.shared.core.analytics.AffiliateAttributionStore
import com.yral.shared.libs.routing.deeplink.engine.RoutingService
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.dsl.module

class AppDIHelper : KoinComponent {
    fun getFeatureFlagManager(): FeatureFlagManager = get()
    fun getRoutingService(): RoutingService = get()
    fun getAffiliateAttributionStore(): AffiliateAttributionStore = get()
}

interface ExternalDependencyProvider {
    fun createOneSignalKMP(): OneSignalKMP
}

fun KoinApplication.installExternalDependencyModule(provider: ExternalDependencyProvider) {
    modules(
        module {
            single { provider.createOneSignalKMP() }
        },
    )
}
