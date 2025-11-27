package com.yral.shared.preferences.di

import com.russhwolf.settings.ExperimentalSettingsApi
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.AsyncPreferencesImpl
import com.yral.shared.preferences.FlowPreferencesImpl
import com.yral.shared.preferences.Preferences
import com.yral.shared.preferences.PreferencesFactory
import com.yral.shared.preferences.stores.AffiliateAttributionStore
import com.yral.shared.preferences.stores.UtmAttributionStore
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val USER_SHARED_PREF_NAME = "YRAL_PREF"
private const val FEED_CACHE_PREF_NAME = "YRAL_FEED_CACHE_PREF"

@OptIn(ExperimentalSettingsApi::class)
val preferencesModule =
    module {
        singleOf(::AsyncPreferencesImpl) { bind<Preferences>() }
        single { PreferencesFactory() }
        single { get<PreferencesFactory>().create(USER_SHARED_PREF_NAME) }
        single<Preferences>(named("FeedCachePreferences")) {
            FlowPreferencesImpl(
                flowSettings =
                    get<PreferencesFactory>().createDataStore(
                        preferenceName = FEED_CACHE_PREF_NAME,
                        appDispatchers = get<AppDispatchers>(),
                    ),
                appDispatchers = get(),
            )
        }
        single { AffiliateAttributionStore(get()) }
        single { UtmAttributionStore(get()) }
    }
