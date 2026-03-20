package com.yral.shared.features.wallet.domain.repository

import com.yral.shared.features.wallet.domain.models.BillingBalance
import com.yral.shared.features.wallet.domain.models.BtcRewardConfig
import com.yral.shared.features.wallet.domain.models.BtcToCurrency
import com.yral.shared.features.wallet.domain.models.DolrPrice
import com.yral.shared.features.wallet.domain.models.GetBalanceResponse
import com.yral.shared.features.wallet.domain.models.Transaction

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
    suspend fun getBalance(userPrincipal: String): GetBalanceResponse
    suspend fun getBillingBalance(recipientId: String): BillingBalance
    suspend fun getTransactions(recipientId: String): List<Transaction>
}
