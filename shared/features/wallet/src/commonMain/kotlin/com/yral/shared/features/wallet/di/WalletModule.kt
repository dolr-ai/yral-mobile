package com.yral.shared.features.wallet.di

import com.yral.shared.features.wallet.analytics.WalletTelemetry
import com.yral.shared.features.wallet.data.WalletDataSource
import com.yral.shared.features.wallet.data.WalletDataSourceImpl
import com.yral.shared.features.wallet.data.WalletRepositoryImpl
import com.yral.shared.features.wallet.domain.GetBtcConversionUseCase
import com.yral.shared.features.wallet.domain.GetDolrUsdPriceUseCase
import com.yral.shared.features.wallet.domain.GetRewardConfigUseCase
import com.yral.shared.features.wallet.domain.GetUserBtcBalanceUseCase
import com.yral.shared.features.wallet.domain.GetUserDolrBalanceUseCase
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.features.wallet.viewmodel.WalletViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val walletModule =
    module {
        factoryOf(::WalletDataSourceImpl).bind<WalletDataSource>()
        factoryOf(::WalletRepositoryImpl).bind<WalletRepository>()
        factoryOf(::WalletTelemetry)
        factoryOf(::GetBtcConversionUseCase)
        factoryOf(::GetUserBtcBalanceUseCase)
        factoryOf(::GetUserDolrBalanceUseCase)
        factoryOf(::GetRewardConfigUseCase)
        factoryOf(::GetDolrUsdPriceUseCase)
        viewModelOf(::WalletViewModel)
    }
