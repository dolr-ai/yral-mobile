package com.yral.shared.app

import com.yral.shared.crashlytics.di.SENTRY_DSN
import com.yral.shared.crashlytics.di.SENTRY_ENVIRONMENT
import com.yral.shared.crashlytics.di.SENTRY_RELEASE
import com.yral.shared.koin.koinInstance
import initializeSentry

fun initializeApp() {
    setupSentry()
}

private fun setupSentry() {
    val dsn = koinInstance.get<String>(SENTRY_DSN)
    val environment = koinInstance.get<String>(SENTRY_ENVIRONMENT)
    val release = koinInstance.get<String>(SENTRY_RELEASE)
    initializeSentry(dsn, environment, release)
}
