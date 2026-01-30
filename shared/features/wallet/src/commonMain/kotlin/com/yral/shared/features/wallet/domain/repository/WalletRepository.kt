package com.yral.shared.features.wallet.domain.repository

import com.yral.shared.features.wallet.domain.models.BtcRewardConfig
import com.yral.shared.features.wallet.domain.models.BtcToCurrency
import com.yral.shared.features.wallet.domain.models.DolrPrice

interface WalletRepository {
    suspend fun getBtcConversionRate(
        idToken: String,
        countryCode: String,
    ): BtcToCurrency
    suspend fun getUserBtcBalance(
        canisterId: String,
        userPrincipal: String,
    ): String
    suspend fun getUserDolrBalance(
        canisterId: String,
        userPrincipal: String,
    ): String
    suspend fun getBtcRewardConfig(): BtcRewardConfig?
    suspend fun getDolrPrice(): DolrPrice
}
