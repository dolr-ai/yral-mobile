package com.yral.shared.features.account.di

import com.yral.shared.core.di.CHAT_SERVER_BASE_URL
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.features.account.analytics.AccountsTelemetry
import com.yral.shared.features.account.domain.useCases.SoftDeleteInfluencerOnBotServerUseCase
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountsModule =
    module {
        factoryOf(::AccountsTelemetry)
        factory {
            SoftDeleteInfluencerOnBotServerUseCase(
                commonApis = get<CommonApis>(),
                preferences = get(),
                chatBaseUrl = get(CHAT_SERVER_BASE_URL),
                dispatchers = get(),
                useCaseFailureListener = get(),
            )
        }
        viewModelOf(::AccountsViewModel)
    }
