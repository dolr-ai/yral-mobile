package com.yral.shared.features.wallet.data

import com.yral.shared.features.wallet.data.models.BillingBalanceResponseDto
import com.yral.shared.features.wallet.data.models.BillingTransactionsResponseDto
import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto
import com.yral.shared.features.wallet.data.models.BtcRewardConfigResponseDto
import com.yral.shared.features.wallet.data.models.DolrPriceResponseDto
import com.yral.shared.features.wallet.data.models.GetBalanceResponseDto

interface WalletDataSource {
    suspend fun getBtcConversionRate(
        idToken: String,
        countryCode: String,
    ): BtcPriceResponseDto
    suspend fun getUserBtcBalance(
        canisterId: String,
        userPrincipal: String,
    ): String
    suspend fun getUserDolrBalance(
        canisterId: String,
        userPrincipal: String,
    ): String
    suspend fun getBtcRewardConfig(): BtcRewardConfigResponseDto
    suspend fun getDolrUsdPrice(): DolrPriceResponseDto
    suspend fun getBalance(userPrincipal: String): GetBalanceResponseDto
    suspend fun getBillingBalance(recipientId: String): BillingBalanceResponseDto
    suspend fun getTransactions(recipientId: String): BillingTransactionsResponseDto
}
