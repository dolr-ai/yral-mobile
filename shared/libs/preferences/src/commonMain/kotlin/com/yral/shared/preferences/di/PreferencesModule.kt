package com.yral.shared.preferences.di

import com.yral.shared.koin.koinInstance
import com.yral.shared.preferences.AsyncPreferencesImpl
import com.yral.shared.preferences.Preferences
import com.yral.shared.preferences.provideSharedPreferences
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private const val USER_SHARED_PREF_NAME = "YRAL_PREF"

val preferencesModule =
    module {
        singleOf(::AsyncPreferencesImpl) { bind<Preferences>() }
        single {
            provideSharedPreferences(
                preferenceName = USER_SHARED_PREF_NAME,
                platformResourcesFactory = koinInstance.get(),
            )
        }
    }
