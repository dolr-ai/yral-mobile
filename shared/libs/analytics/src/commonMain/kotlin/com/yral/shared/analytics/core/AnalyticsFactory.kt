package com.yral.shared.analytics.core

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject

class AnalyticsFactory(
    private val providers: List<AnalyticsProvider> = emptyList(),
    private val coreService: CoreService? = null,
) {
    private val analyticsManager by lazy {
        AnalyticsManager(
            providers = providers,
            coreService = coreService,
        )
    }

    fun build(): AnalyticsManager = analyticsManager

    @OptIn(InternalCoroutinesApi::class)
    companion object : SynchronizedObject() {
        @Volatile
        private var instance: AnalyticsFactory? = null

        fun getInstance(
            providers: List<AnalyticsProvider> = emptyList(),
            coreService: CoreService? = null,
        ): AnalyticsFactory =
            instance ?: synchronized(this) {
                instance ?: AnalyticsFactory(providers, coreService).also { instance = it }
            }
    }
}
