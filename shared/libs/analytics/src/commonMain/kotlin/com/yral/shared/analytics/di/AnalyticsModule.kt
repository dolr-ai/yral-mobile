package com.yral.shared.analytics.di

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.events.shouldSendToFacebook
import com.yral.shared.analytics.events.shouldSendToYralBE
import com.yral.shared.analytics.providers.bigquery.BigQueryAnalyticsProvider
import com.yral.shared.analytics.providers.bigquery.BigQueryEventsApiService
import com.yral.shared.analytics.providers.facebook.FacebookAnalyticsProvider
import com.yral.shared.analytics.providers.firebase.FirebaseAnalyticsProvider
import com.yral.shared.analytics.providers.mixpanel.MixpanelAnalyticsProvider
import com.yral.shared.analytics.providers.onesignal.OneSignalAnalyticsProvider
import com.yral.shared.analytics.providers.yral.AnalyticsApiService
import com.yral.shared.analytics.providers.yral.CoreService
import com.yral.shared.koin.koinInstance
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.app
import kotlinx.serialization.json.buildJsonObject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val analyticsModule =
    module {
        singleOf(::AnalyticsApiService)
        singleOf(::EventToMapConverter)
        singleOf(::BigQueryEventsApiService)
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
                utmAttributionStore = get(),
            )
        }
        single {
            OneSignalAnalyticsProvider(
                eventFilter = { false },
                mapConverter = get(),
                appId = get<String>(ONESIGNAL_APP_ID),
                oneSignal = get(),
            )
        }
        single {
            FacebookAnalyticsProvider(
                eventFilter = { it.shouldSendToFacebook() },
                mapConverter = get(),
            )
        }
        single {
            val isDebug: Boolean = get(IS_DEBUG)
            BigQueryAnalyticsProvider(
                apiService = get(),
                crashlyticsService = get(),
                json = get(),
                eventFilter = { !it.shouldSendToYralBE() },
                extraFieldsProvider = { buildJsonObject { } },
                dryRun = isDebug,
                log = { },
            )
        }
        single {
            AnalyticsManager()
                .addProvider(get<FirebaseAnalyticsProvider>())
                .addProvider(get<MixpanelAnalyticsProvider>())
                .addProvider(get<OneSignalAnalyticsProvider>())
                .addProvider(get<FacebookAnalyticsProvider>())
                .addProvider(get<BigQueryAnalyticsProvider>())
                .setCoreService(get<CoreService>())
        }
    }

public fun getAnalyticsManager(): AnalyticsManager = koinInstance.get()
