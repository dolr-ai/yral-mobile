package com.yral.android.di

import com.yral.shared.http.createClient
import com.yral.shared.koin.koinInstance
import org.koin.dsl.module

internal val networkModule =
    module {
        single { createClient(koinInstance.get()) }
    }
