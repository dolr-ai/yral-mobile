package com.yral.shared.app.di

import com.yral.shared.analytics.di.analyticsModule
import com.yral.shared.core.di.coreModule
import com.yral.shared.crashlytics.di.crashlyticsModule
import com.yral.shared.http.di.networkModule
import com.yral.shared.preferences.di.preferencesModule
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
        )
    }
}
