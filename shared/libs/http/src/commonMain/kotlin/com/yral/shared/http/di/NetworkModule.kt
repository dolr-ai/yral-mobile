package com.yral.shared.http.di

import com.yral.shared.http.ConsoleLogger
import com.yral.shared.http.createClient
import com.yral.shared.http.createClientJson
import org.koin.dsl.module

val networkModule =
    module {
        single { createClientJson() }
        single {
            createClient(
                get(),
                get(),
                get(),
            )
        }
        single { ConsoleLogger() }
    }
