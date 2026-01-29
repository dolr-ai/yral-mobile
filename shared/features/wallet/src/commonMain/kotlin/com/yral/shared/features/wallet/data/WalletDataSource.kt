package com.yral.shared.features.wallet.data

import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto
import com.yral.shared.features.wallet.data.models.BtcRewardConfigResponseDto
import com.yral.shared.features.wallet.data.models.DolrPriceResponseDto

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
}
