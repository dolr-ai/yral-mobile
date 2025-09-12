package com.yral.shared.app.di

import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.analytics.di.analyticsModule
import com.yral.shared.core.di.coreModule
import com.yral.shared.crashlytics.di.crashlyticsModule
import com.yral.shared.http.di.networkModule
import com.yral.shared.libs.routing.deeplink.engine.RoutingService
import com.yral.shared.preferences.di.preferencesModule
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    startKoin {
        // Forbid definition override
        allowOverride(false)
        appDeclaration()
        modules(
            dispatchersModule,
            platformModule,
            coreModule,
            preferencesModule,
            analyticsModule,
            crashlyticsModule,
            networkModule,
            featureFlagModule,
            routingModule,
            sharingModule,
        )
    }
}

class AppDIHelper : KoinComponent {
    fun getFeatureFlagManager(): FeatureFlagManager = get()
    fun getRoutingService(): RoutingService = get()
}
