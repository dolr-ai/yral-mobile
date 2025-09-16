package com.yral.shared.features.wallet.data

import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto

interface WalletDataSource {
    suspend fun getBtcInInr(idToken: String): BtcPriceResponseDto
    suspend fun getUserBtcBalance(
        canisterId: String,
        userPrincipal: String,
    ): String
    suspend fun getUserDolrBalance(
        canisterId: String,
        userPrincipal: String,
    ): String
}
