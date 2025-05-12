package com.yral.shared.analytics.di

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.shouldSendToYralBE
import com.yral.shared.analytics.providers.FirebaseAnalyticsProvider
import com.yral.shared.analytics.providers.yral.AnalyticsApiService
import com.yral.shared.analytics.providers.yral.CoreService
import com.yral.shared.core.utils.toMap
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
        single {
            FirebaseAnalyticsProvider(
                eventFilter = { !it.shouldSendToYralBE() },
                mapConverter = { event ->
                    val json: Json = get()
                    json.encodeToJsonElement(event).toMap().mapValues { it.value != null }
                },
            )
        }
        single {
            AnalyticsManager()
                .addProvider(get<FirebaseAnalyticsProvider>())
                .setCoreService(get<CoreService>())
        }
    }
