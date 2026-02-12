package com.yral.shared.crashlytics.core

class CrashlyticsManager(
    private val providers: List<CrashlyticsProvider> = emptyList(),
) {
    fun recordException(exception: Exception) {
        providers.forEach {
            it.recordException(exception)
        }
    }

    fun recordException(
        exception: Exception,
        type: ExceptionType,
    ) {
        providers.forEach {
            it.recordException(exception, type)
        }
    }

    fun setUserId(id: String) {
        providers.forEach {
            it.setUserId(id)
        }
    }

    fun logMessage(message: String) {
        providers.forEach {
            it.logMessage(message)
        }
    }
}
