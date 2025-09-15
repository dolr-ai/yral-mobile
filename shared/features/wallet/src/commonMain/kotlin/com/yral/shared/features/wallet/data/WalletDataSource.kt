package com.yral.shared.features.wallet.data

import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto

interface WalletDataSource {
    suspend fun getBtcInInr(idToken: String): BtcPriceResponseDto
    suspend fun getUserBtcBalance(userPrincipal: String): String
}
