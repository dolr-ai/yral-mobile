package com.yral.shared.analytics.di

import com.russhwolf.settings.Settings
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.DeviceInstallIdStore
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.adTracking.GetADIDUseCase
import com.yral.shared.analytics.events.shouldSendToBranch
import com.yral.shared.analytics.events.shouldSendToFacebook
import com.yral.shared.analytics.providers.branch.BranchAnalyticsProvider
import com.yral.shared.analytics.providers.facebook.FacebookAnalyticsProvider
import com.yral.shared.analytics.providers.firebase.FirebaseAnalyticsProvider
import com.yral.shared.analytics.providers.mixpanel.MixpanelAnalyticsProvider
import com.yral.shared.analytics.providers.snowplow.SnowplowAnalyticsProvider
import com.yral.shared.koin.koinInstance
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val analyticsModule =
    module {
        singleOf(::EventToMapConverter)
        single { DeviceInstallIdStore(get<Settings>()) }
        single {
            FirebaseAnalyticsProvider(
                eventFilter = { true },
                mapConverter = get(),
            )
        }
        single {
            MixpanelAnalyticsProvider(
                eventFilter = { true },
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
            BranchAnalyticsProvider(
                eventFilter = { it.shouldSendToBranch() },
                mapConverter = get(),
            )
        }
        single {
            SnowplowAnalyticsProvider(
                eventFilter = { true },
                mapConverter = get(),
                appId = get(SNOWPLOW_APP_ID),
            )
        }
        single {
            AnalyticsManager(
                providers =
                    listOf(
                        get<FirebaseAnalyticsProvider>(),
                        get<MixpanelAnalyticsProvider>(),
                        get<FacebookAnalyticsProvider>(),
                        get<BranchAnalyticsProvider>(),
                        get<SnowplowAnalyticsProvider>(),
                    ),
                deviceInstallIdStore = get(),
            )
        }
        singleOf(::GetADIDUseCase)
    }

fun getAnalyticsManager(): AnalyticsManager = koinInstance.get()
