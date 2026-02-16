package com.yral.shared.features.account.di

import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.features.account.analytics.AccountsTelemetry
import com.yral.shared.features.account.domain.useCases.SoftDeleteInfluencerOnBotServerUseCase
import com.yral.shared.features.account.viewmodel.AccountEnv
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountsModule =
    module {
        factoryOf(::AccountsTelemetry)
        factory { AccountEnv(isDebug = get(IS_DEBUG)) }
        factory {
            SoftDeleteInfluencerOnBotServerUseCase(
                commonApis = get<CommonApis>(),
                preferences = get(),
                isDebug = get(IS_DEBUG),
                dispatchers = get(),
                useCaseFailureListener = get(),
            )
        }
        viewModelOf(::AccountsViewModel)
    }
