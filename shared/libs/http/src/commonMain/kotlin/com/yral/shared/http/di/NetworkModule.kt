package com.yral.shared.http.di

import com.yral.shared.http.createClient
import com.yral.shared.http.createClientJson
import com.yral.shared.koin.koinInstance
import org.koin.dsl.module

val networkModule =
    module {
        single { createClientJson() }
        single {
            createClient(
                koinInstance.get(),
                koinInstance.get(),
            )
        }
    }
