package com.yral.shared.features.root.di

import com.yral.shared.features.root.viewmodels.RootViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val rootModule: Module =
    module {
        viewModelOf(::RootViewModel)
    }
