package com.yral.shared.features.auth.di

import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.auth.DefaultAuthClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule =
    module {
        singleOf(::DefaultAuthClient) { bind<AuthClient>() }
    }
