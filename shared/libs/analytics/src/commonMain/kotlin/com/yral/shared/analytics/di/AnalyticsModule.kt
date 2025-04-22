package com.yral.shared.analytics.di

import com.yral.shared.analytics.core.ApiClient
import com.yral.shared.analytics.core.CoreService
import org.koin.dsl.module

private const val ANALYTICS_BASE_URL = "https://yral.com"
private const val ANALYTICS_BATCH_SIZE = 10
private const val ANALYTICS_FLUSH_MS = 120000L

val analyticsModule =
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
    }
