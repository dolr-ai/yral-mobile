package com.yral.android.di

import com.yral.shared.preferences.AsyncPreferencesImpl
import com.yral.shared.preferences.Preferences
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val libModule =
    module {
        singleOf(::AsyncPreferencesImpl) { bind<Preferences>() }
    }
