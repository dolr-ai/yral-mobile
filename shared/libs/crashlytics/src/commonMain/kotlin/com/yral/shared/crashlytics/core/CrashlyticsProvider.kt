package com.yral.shared.crashlytics.core

interface CrashlyticsProvider {
    val name: String
    fun recordException(exception: Exception)
}
