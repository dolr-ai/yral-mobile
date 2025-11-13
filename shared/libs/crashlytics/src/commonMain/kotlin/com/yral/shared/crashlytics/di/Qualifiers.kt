package com.yral.shared.crashlytics.di

import org.koin.core.qualifier.named

val SENTRY_DSN = named("sentryDsn")
val SENTRY_ENVIRONMENT = named("sentryEnvironment")
val SENTRY_RELEASE = named("sentryRelease")
