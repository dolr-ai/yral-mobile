package com.yral.shared.analytics.di

import com.russhwolf.settings.Settings
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.DeviceInstallIdStore
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.adTracking.GetADIDUseCase
import com.yral.shared.analytics.events.shouldSendToBranch
import com.yral.shared.analytics.events.shouldSendToFacebook
import com.yral.shared.analytics.events.shouldSendToYralBE
import com.yral.shared.analytics.events.shouldSendViaCore
import com.yral.shared.analytics.providers.branch.BranchAnalyticsProvider
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
        single { DeviceInstallIdStore(get<Settings>()) }
        single {
            val isDebug: Boolean = get(IS_DEBUG)
            CoreService(
                analyticsApiService = get(),
                crashlyticsManager = get(),
                eventFilter = { it.shouldSendViaCore(isDebug) },
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
            BranchAnalyticsProvider(
                eventFilter = { it.shouldSendToBranch() },
                mapConverter = get(),
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
                    ),
                coreService = get<CoreService>(),
                deviceInstallIdStore = get(),
            )
        }
        singleOf(::GetADIDUseCase)
    }

fun getAnalyticsManager(): AnalyticsManager = koinInstance.get()
