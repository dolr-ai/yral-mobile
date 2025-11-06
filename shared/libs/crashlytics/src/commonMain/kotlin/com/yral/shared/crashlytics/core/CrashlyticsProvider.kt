package com.yral.shared.crashlytics.core

interface CrashlyticsProvider {
    val name: String
    fun recordException(exception: Exception)
    fun recordException(
        exception: Exception,
        type: ExceptionType,
    )
    fun logMessage(message: String)

    fun setUserId(id: String)
}

enum class ExceptionType {
    UNKNOWN,
    RUST,
    AUTH,
    FEED,
    DEEPLINK,
}
