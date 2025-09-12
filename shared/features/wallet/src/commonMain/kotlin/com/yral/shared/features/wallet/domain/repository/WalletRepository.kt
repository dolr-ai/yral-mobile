package com.yral.shared.features.wallet.domain.repository

import com.yral.shared.features.wallet.domain.models.BtcInInr
import com.yral.shared.features.wallet.domain.models.UserBtcBalance

interface WalletRepository {
    suspend fun getBtcInInr(idToken: String): BtcInInr
    suspend fun getUserBtcBalance(userPrincipal: String): UserBtcBalance
}
