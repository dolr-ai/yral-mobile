package com.yral.shared.features.account.di

import com.yral.shared.features.account.analytics.AccountsTelemetry
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Dependencies as AccountsDependencies

val accountsModule =
    module {
        factoryOf(::AccountsTelemetry)
        factoryOf(::AccountsDependencies)
        viewModelOf(::AccountsViewModel)
    }
