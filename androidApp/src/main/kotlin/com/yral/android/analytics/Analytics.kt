package com.yral.android.analytics

import com.yral.shared.analytics.core.AnalyticsManager
import com.yral.shared.analytics.core.ApiClient
import com.yral.shared.analytics.core.CoreService
import com.yral.shared.core.PlatformResourcesHolder.platformResources
import com.yral.shared.koin.koinInstance

fun initAnalyticsManager() {
    val analyticsManager: AnalyticsManager = koinInstance.get()
    analyticsManager.addProvider(
        FirebaseAnalyticsProvider(
            context = platformResources.applicationContext,
            eventFilter = { event ->
                // Only track specific events to Firebase
                event.name.startsWith("firebase_")
            },
        ),
    )
    analyticsManager.setCoreService(
        CoreService(
            apiClient = ApiClient("https://yral.com"),
            batchSize = 10,
            autoFlushEvents = true,
            autoFlushIntervalMs = 120000,
        ),
    )
}
