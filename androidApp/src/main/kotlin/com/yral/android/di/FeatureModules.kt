package com.yral.android.di

import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.auth.DefaultAuthClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val authModule =
    module {
        singleOf(::DefaultAuthClient) { bind<AuthClient>() }
    }
