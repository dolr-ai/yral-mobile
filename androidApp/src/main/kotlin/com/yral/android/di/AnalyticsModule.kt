package com.yral.android.di

import com.yral.android.analytics.FirebaseAnalyticsProvider
import com.yral.shared.analytics.core.AnalyticsManager
import com.yral.shared.analytics.core.CoreService
import com.yral.shared.core.platform.PlatformResourcesFactory
import org.koin.dsl.module

internal val appAnalyticsModule =
    module {
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
