package com.yral.shared.features.root.di

import com.yral.shared.features.root.analytics.RootTelemetry
import com.yral.shared.features.root.viewmodels.HomeViewModel
import com.yral.shared.features.root.viewmodels.RootViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val rootModule: Module =
    module {
        singleOf(::RootViewModel)
        singleOf(::HomeViewModel)
        factoryOf(::RootTelemetry)
    }
