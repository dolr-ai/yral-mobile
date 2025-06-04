package com.yral.shared.crashlytics.core

class CrashlyticsManager(
    private val providers: List<CrashlyticsProvider> = emptyList(),
) {
    internal fun addProvider(provider: CrashlyticsProvider) =
        CrashlyticsManager(
            providers = providers + provider,
        )

    fun recordException(exception: Exception) {
        providers.forEach {
            it.recordException(exception)
        }
    }

    fun setUserId(id: String) {
        providers.forEach {
            it.setUserId(id)
        }
    }
}
