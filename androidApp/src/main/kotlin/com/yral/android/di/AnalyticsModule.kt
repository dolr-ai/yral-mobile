package com.yral.android.di

import com.yral.android.analytics.FirebaseAnalyticsProvider
import com.yral.shared.analytics.core.AnalyticsManager
import com.yral.shared.analytics.core.ApiClient
import com.yral.shared.analytics.core.CoreService
import com.yral.shared.core.PlatformResourcesFactory
import org.koin.dsl.module

private const val ANALYTICS_BASE_URL = "https://yral.com"
private const val ANALYTICS_BATCH_SIZE = 10
private const val ANALYTICS_FLUSH_MS = 120000L

internal val analyticsModule =
    module {
        single { ApiClient(ANALYTICS_BASE_URL) }
        single {
            CoreService(
                apiClient = get(),
                batchSize = ANALYTICS_BATCH_SIZE,
                autoFlushEvents = true,
                autoFlushIntervalMs = ANALYTICS_FLUSH_MS,
            )
        }
        single {
            FirebaseAnalyticsProvider(
                context = get<PlatformResourcesFactory>().resources().applicationContext,
                eventFilter = { it.name.startsWith("firebase_") },
            )
        }
        single {
            AnalyticsManager().apply {
                addProvider(get<FirebaseAnalyticsProvider>())
                setCoreService(get<CoreService>())
            }
        }
    }
