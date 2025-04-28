package com.yral.shared.features.account.di

import com.yral.shared.features.account.viewmodel.AccountsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountsModule =
    module {
        viewModelOf(::AccountsViewModel)
    }
