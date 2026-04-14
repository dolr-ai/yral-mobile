package com.yral.shared.crashlytics.di

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.CrashlyticsProvider
import com.yral.shared.crashlytics.core.createCrashlyticsProvider
import org.koin.dsl.module

val crashlyticsModule =
    module {
        single<CrashlyticsProvider> { createCrashlyticsProvider() }
        single {
            CrashlyticsManager(
                providers =
                    listOf(
                        get(),
                    ),
            )
        }
    }
