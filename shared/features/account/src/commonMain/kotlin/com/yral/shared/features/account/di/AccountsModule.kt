package com.yral.shared.features.account.di

import com.yral.shared.features.account.viewmodel.AccountsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val accountsModule =
    module {
        singleOf(::AccountsViewModel)
    }
