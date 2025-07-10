package com.yral.shared.analytics.di

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.events.shouldSendToFacebook
import com.yral.shared.analytics.events.shouldSendToYralBE
import com.yral.shared.analytics.providers.facebook.FacebookAnalyticsProvider
import com.yral.shared.analytics.providers.firebase.FirebaseAnalyticsProvider
import com.yral.shared.analytics.providers.mixpanel.MixpanelAnalyticsProvider
import com.yral.shared.analytics.providers.yral.AnalyticsApiService
import com.yral.shared.analytics.providers.yral.CoreService
import com.yral.shared.koin.koinInstance
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.app
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val analyticsModule =
    module {
        singleOf(::AnalyticsApiService)
        singleOf(::EventToMapConverter)
        single {
            CoreService(
                analyticsApiService = get(),
                crashlyticsManager = get(),
                eventFilter = { it.shouldSendToYralBE() },
            )
        }
        factory { Firebase.analytics(Firebase.app) }
        single {
            FirebaseAnalyticsProvider(
                firebaseAnalytics = get(),
                eventFilter = { !it.shouldSendToYralBE() },
                mapConverter = get(),
            )
        }
        single {
            MixpanelAnalyticsProvider(
                eventFilter = { !it.shouldSendToYralBE() },
                mapConverter = get(),
                token = get<String>(MIXPANEL_TOKEN),
            )
        }
        single {
            FacebookAnalyticsProvider(
                eventFilter = { it.shouldSendToFacebook() },
                mapConverter = get(),
            )
        }
        single {
            AnalyticsManager()
                .addProvider(get<FirebaseAnalyticsProvider>())
                .addProvider(get<MixpanelAnalyticsProvider>())
                .addProvider(get<FacebookAnalyticsProvider>())
                .setCoreService(get<CoreService>())
        }
    }

public fun getAnalyticsManager(): AnalyticsManager = koinInstance.get()
