package com.yral.shared.preferences.di

import com.yral.shared.preferences.AffiliateAttributionStore
import com.yral.shared.preferences.AsyncPreferencesImpl
import com.yral.shared.preferences.Preferences
import com.yral.shared.preferences.PreferencesFactory
import com.yral.shared.preferences.UtmAttributionStore
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private const val USER_SHARED_PREF_NAME = "YRAL_PREF"

val preferencesModule =
    module {
        singleOf(::AsyncPreferencesImpl) { bind<Preferences>() }
        single { PreferencesFactory() }
        single { get<PreferencesFactory>().create(USER_SHARED_PREF_NAME) }
        single { AffiliateAttributionStore(get()) }
        single { UtmAttributionStore(get()) }
    }
