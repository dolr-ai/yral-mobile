package com.yral.android.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration) {
    startKoin {
        // Forbid definition override
        allowOverride(false)
        appDeclaration()
        modules(
            coreModule,
            libModule,
            analyticsModule,
            networkModule,
            authModule,
            rustModule,
        )
    }
}
