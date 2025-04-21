package com.yral.android.di

import com.yral.shared.http.HttpClientFactory
import com.yral.shared.koin.koinInstance
import org.koin.dsl.module

internal val networkModule =
    module {
        single { HttpClientFactory.getInstance(koinInstance.get()).build() }
    }
