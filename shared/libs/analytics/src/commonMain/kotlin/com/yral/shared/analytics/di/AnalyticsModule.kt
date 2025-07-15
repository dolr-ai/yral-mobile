package com.yral.shared.analytics.di

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.EventData
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
import kotlinx.coroutines.NonCancellable.get
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
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
            val mpConverter: (EventData) -> Map<String, Any> = { event ->
                val json: Json = get()
                json
                    .encodeToJsonElement(event)
                    .toMap()
                    .mapValues { (_, value) ->
                        when (value) {
                            is JsonPrimitive ->
                                when {
                                    value.isString -> value.content
                                    value.booleanOrNull != null -> value.boolean
                                    value.intOrNull != null -> value.int
                                    value.longOrNull != null -> value.long
                                    value.doubleOrNull != null -> value.double
                                    else -> value.content
                                }

                            else -> value.toString()
                        }
                    }.filterValues { it != null }
                    .mapValues { it.value as Any }
            }
            MixpanelAnalyticsProvider(
                eventFilter = { !it.shouldSendToYralBE() },
                mapConverter = mpConverter,
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
