package com.yral.shared.crashlytics.core

import dev.gitlive.firebase.crashlytics.FirebaseCrashlytics

internal class FirebaseCrashlyticsProvider(
    private val crashlytics: FirebaseCrashlytics,
) : CrashlyticsProvider {
    override val name: String
        get() = "firebase"

    override fun recordException(exception: Exception) {
        crashlytics.recordException(exception)
    }

    override fun logMessage(message: String) {
        crashlytics.log(message)
    }

    override fun setUserId(id: String) {
        crashlytics.setUserId(id)
    }
}
