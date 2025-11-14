package com.yral.shared.crashlytics.di

import com.yral.shared.crashlytics.core.CrashlyticsLogWriter
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.FirebaseCrashlyticsProvider
import com.yral.shared.crashlytics.core.SentryCrashlyticsProvider
import com.yral.shared.crashlytics.di.SENTRY_DSN
import com.yral.shared.crashlytics.di.SENTRY_ENVIRONMENT
import com.yral.shared.crashlytics.di.SENTRY_RELEASE
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.crashlytics.crashlytics
import io.sentry.kotlin.multiplatform.Sentry
import org.koin.core.qualifier.named
import org.koin.dsl.module

private var sentryInitialized = false

val crashlyticsModule =
    module {
        factory { Firebase.crashlytics(Firebase.app) }
        single { FirebaseCrashlyticsProvider(get()) }
        single {
            val dsn = get<String>(SENTRY_DSN)
            val environment = get<String>(SENTRY_ENVIRONMENT)
            val release = get<String>(SENTRY_RELEASE)
            if (!sentryInitialized) {
                require(dsn.isNotBlank()) { "SENTRY_DSN must not be blank" }
                Sentry.init { options ->
                    options.dsn = dsn
                    options.environment = environment
                    options.release = release
                }
                sentryInitialized = true
            }
            SentryCrashlyticsProvider()
        }
        single {
            CrashlyticsManager()
                .addProvider(get<FirebaseCrashlyticsProvider>())
                .addProvider(get<SentryCrashlyticsProvider>())
        }
        single(named("crashlyticsLogWriter")) { CrashlyticsLogWriter(get()) }
    }
