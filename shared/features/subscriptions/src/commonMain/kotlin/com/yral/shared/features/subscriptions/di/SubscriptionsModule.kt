package com.yral.shared.features.subscriptions.di

import com.yral.shared.features.subscriptions.analytics.SubscriptionTelemetry
import com.yral.shared.features.subscriptions.domain.QueryPurchaseUseCase
import com.yral.shared.features.subscriptions.viewmodel.SubscriptionViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val subscriptionsModule =
    module {
        factoryOf(::QueryPurchaseUseCase)
        factoryOf(::SubscriptionTelemetry)
        viewModelOf(::SubscriptionViewModel)
    }
