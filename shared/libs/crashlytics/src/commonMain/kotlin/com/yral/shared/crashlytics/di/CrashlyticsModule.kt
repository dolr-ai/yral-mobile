package com.yral.shared.crashlytics.di

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.FirebaseCrashlyticsProvider
import com.yral.shared.crashlytics.core.SentryCrashlyticsProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.crashlytics.crashlytics
import org.koin.dsl.module

val crashlyticsModule =
    module {
        factory { Firebase.crashlytics(Firebase.app) }
        single { FirebaseCrashlyticsProvider(get()) }
        single { SentryCrashlyticsProvider() }
        single {
            CrashlyticsManager(
                providers =
                    listOf(
                        get<FirebaseCrashlyticsProvider>(),
                        get<SentryCrashlyticsProvider>(),
                    ),
            )
        }
    }
