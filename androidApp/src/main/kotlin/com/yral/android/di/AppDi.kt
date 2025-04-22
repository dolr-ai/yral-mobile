package com.yral.android.di

import com.yral.shared.analytics.di.analyticsModule
import com.yral.shared.core.di.coreModule
import com.yral.shared.features.auth.di.authModule
import com.yral.shared.features.feed.di.feedModule
import com.yral.shared.http.di.networkModule
import com.yral.shared.preferences.di.preferencesModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration) {
    startKoin {
        // Forbid definition override
        allowOverride(false)
        appDeclaration()
        modules(
            coreModule,
            preferencesModule,
            analyticsModule,
            networkModule,
            rustModule,
        )
        modules(
            authModule,
            feedModule,
        )
    }
}
