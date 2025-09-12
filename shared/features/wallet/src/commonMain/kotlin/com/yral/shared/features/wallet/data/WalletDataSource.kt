package com.yral.shared.features.wallet.data

import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto
import com.yral.shared.features.wallet.data.models.GetBalanceResponseDto

interface WalletDataSource {
    suspend fun getBtcInInr(idToken: String): BtcPriceResponseDto
    suspend fun getUserBtcBalance(userPrincipal: String): GetBalanceResponseDto
}
