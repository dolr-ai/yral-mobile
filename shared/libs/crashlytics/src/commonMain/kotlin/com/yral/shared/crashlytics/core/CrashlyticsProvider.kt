package com.yral.shared.crashlytics.core

interface CrashlyticsProvider {
    val name: String
    fun recordException(exception: Exception)
    fun logMessage(message: String)

    fun setUserId(id: String)
}
