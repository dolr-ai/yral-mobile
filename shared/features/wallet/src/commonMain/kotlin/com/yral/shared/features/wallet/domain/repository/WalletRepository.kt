package com.yral.shared.features.wallet.domain.repository

import com.yral.shared.features.wallet.domain.models.BtcInInr

interface WalletRepository {
    suspend fun getBtcInInr(idToken: String): BtcInInr
    suspend fun getUserBtcBalance(userPrincipal: String): String
}
