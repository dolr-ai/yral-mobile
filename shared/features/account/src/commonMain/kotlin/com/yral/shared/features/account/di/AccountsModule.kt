package com.yral.shared.features.account.di

import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.features.account.analytics.AccountsTelemetry
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val accountsModule =
    module {
        factoryOf(::AccountsTelemetry)
        viewModel {
            AccountsViewModel(
                appDispatchers = get(),
                authClientFactory = get(),
                sessionManager = get(),
                deleteAccountUseCase = get(),
                accountsTelemetry = get(),
                flagManager = get(),
                firebaseStorage = get(),
                preferences = get(),
                httpClient = get(),
                isDebug = get(IS_DEBUG),
                registerNotificationTokenUseCase = get(),
                deregisterNotificationTokenUseCase = get(),
            )
        }
    }
