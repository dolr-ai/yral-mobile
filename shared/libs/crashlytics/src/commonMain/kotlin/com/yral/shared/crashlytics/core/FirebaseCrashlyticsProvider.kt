package com.yral.shared.crashlytics.core

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

internal class FirebaseCrashlyticsProvider : CrashlyticsProvider {
    override val name: String
        get() = "firebase"

    override fun recordException(exception: Exception) {
        Firebase.crashlytics.recordException(exception)
    }

    override fun logMessage(message: String) {
        Firebase.crashlytics.log(message)
    }

    override fun setUserId(id: String) {
        Firebase.crashlytics.setUserId(id)
    }
}
