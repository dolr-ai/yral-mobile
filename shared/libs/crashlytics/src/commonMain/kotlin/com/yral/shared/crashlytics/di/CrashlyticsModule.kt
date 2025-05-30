package com.yral.shared.crashlytics.di

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.FirebaseCrashlyticsProvider
import org.koin.dsl.module

val crashlyticsModule =
    module {
        single { FirebaseCrashlyticsProvider() }
        single {
            CrashlyticsManager()
                .addProvider(get<FirebaseCrashlyticsProvider>())
        }
    }
