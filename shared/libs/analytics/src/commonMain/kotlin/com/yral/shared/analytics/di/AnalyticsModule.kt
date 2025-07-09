package com.yral.shared.analytics.di

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.shouldSendToFacebook
import com.yral.shared.analytics.events.shouldSendToYralBE
import com.yral.shared.analytics.providers.facebook.FacebookAnalyticsProvider
import com.yral.shared.analytics.providers.firebase.FirebaseAnalyticsProvider
import com.yral.shared.analytics.providers.mixpanel.MixpanelAnalyticsProvider
import com.yral.shared.analytics.providers.yral.AnalyticsApiService
import com.yral.shared.analytics.providers.yral.CoreService
import com.yral.shared.core.utils.toMap
import com.yral.shared.koin.koinInstance
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.app
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val analyticsModule =
    module {
        singleOf(::AnalyticsApiService)
        single {
            CoreService(
                get(),
                get(),
                eventFilter = { it.shouldSendToYralBE() },
            )
        }
        factory { Firebase.analytics(Firebase.app) }
        single {
            FirebaseAnalyticsProvider(
                firebaseAnalytics = get(),
                eventFilter = { !it.shouldSendToYralBE() },
                mapConverter = { event ->
                    val json: Json = get()
                    json.encodeToJsonElement(event).toMap().mapValues { it.value != null }
                },
            )
        }
        single {
            MixpanelAnalyticsProvider(
                eventFilter = { !it.shouldSendToYralBE() },
                mapConverter = { event ->
                    val json: Json = get()
                    json.encodeToJsonElement(event).toMap().mapValues { it.value.toString() }
                },
                token = get<String>(MIXPANEL_TOKEN),
            )
        }
        single {
            FacebookAnalyticsProvider(
                eventFilter = { it.shouldSendToFacebook() },
                mapConverter = { event ->
                    val json: Json = get()
                    json.encodeToJsonElement(event).toMap().mapValues { it.value.toString() }
                },
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
