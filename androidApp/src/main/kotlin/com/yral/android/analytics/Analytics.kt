package com.yral.android.analytics

import com.yral.shared.analytics.core.AnalyticsFactory
import com.yral.shared.analytics.core.AnalyticsManager
import com.yral.shared.analytics.core.ApiClient
import com.yral.shared.analytics.core.CoreService
import com.yral.shared.core.PlatformResourcesHolder.platformResources

fun provideAnalyticsManager(): AnalyticsManager =
    AnalyticsFactory(
        providers =
            listOf(
                FirebaseAnalyticsProvider(
                    context = platformResources.applicationContext,
                    eventFilter = { event ->
                        // Only track specific events to Firebase
                        event.name.startsWith("firebase_")
                    },
                ),
            ),
        coreService =
            CoreService(
                apiClient = ApiClient("https://yral.com"),
                batchSize = 10,
                autoFlushEvents = true,
                autoFlushIntervalMs = 120000,
            ),
    ).build()
