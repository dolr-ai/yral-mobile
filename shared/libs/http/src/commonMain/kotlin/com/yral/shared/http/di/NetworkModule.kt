package com.yral.shared.http.di

import com.yral.shared.http.HttpLogger
import com.yral.shared.http.createClient
import com.yral.shared.http.createClientJson
import org.koin.core.scope.Scope
import org.koin.dsl.module

expect fun Scope.platformContext(): Any

val networkModule =
    module {
        single { createClientJson() }
        single {
            createClient(
                platformContext(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single { HttpLogger(get(), get()) }
    }
