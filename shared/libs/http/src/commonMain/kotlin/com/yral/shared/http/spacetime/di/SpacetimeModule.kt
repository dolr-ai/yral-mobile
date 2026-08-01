package com.yral.shared.http.spacetime.di

import com.yral.shared.http.spacetime.SpacetimeDBRemoteDataSource
import org.koin.dsl.module

val spacetimeModule = module {
    single { SpacetimeDBRemoteDataSource(get(), get(), get()) }
}