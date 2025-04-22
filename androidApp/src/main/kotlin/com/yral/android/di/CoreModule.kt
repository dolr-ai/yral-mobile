package com.yral.android.di

import com.yral.shared.core.AppDispatchers
import com.yral.shared.core.PlatformResourcesFactory
import com.yral.shared.koin.koinInstance
import com.yral.shared.preferences.provideSharedPreferences
import org.koin.dsl.module

private const val USER_SHARED_PREF_NAME = "YRAL_PREF"

internal val coreModule =
    module {
        single { PlatformResourcesFactory() }
        single { AppDispatchers() }
        single {
            provideSharedPreferences(
                preferenceName = USER_SHARED_PREF_NAME,
                platformResourcesFactory = koinInstance.get(),
            )
        }
    }
