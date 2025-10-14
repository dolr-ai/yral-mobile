package com.yral.shared.features.wallet.data

import com.yral.shared.features.wallet.data.models.toDomain
import com.yral.shared.features.wallet.domain.models.BtcRewardConfig
import com.yral.shared.features.wallet.domain.models.BtcToCurrency
import com.yral.shared.features.wallet.domain.repository.WalletRepository

class WalletRepositoryImpl(
    private val dataSource: WalletDataSource,
) : WalletRepository {
    override suspend fun getBtcConversionRate(
        idToken: String,
        countryCode: String,
    ): BtcToCurrency =
        dataSource
            .getBtcConversionRate(idToken, countryCode)
            .toDomain()

    override suspend fun getUserBtcBalance(
        canisterId: String,
        userPrincipal: String,
    ): String =
        dataSource
            .getUserBtcBalance(canisterId, userPrincipal)

    override suspend fun getUserDolrBalance(
        canisterId: String,
        userPrincipal: String,
    ): String =
        dataSource
            .getUserDolrBalance(canisterId, userPrincipal)

    override suspend fun getBtcRewardConfig(): BtcRewardConfig? =
        dataSource
            .getBtcRewardConfig()
            .config
            ?.toDomain()
}
