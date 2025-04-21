package com.yral.android.di

import com.yral.shared.analytics.core.AnalyticsManager
import org.koin.dsl.module

internal val analyticsModule =
    module {
        single { AnalyticsManager() }
    }
