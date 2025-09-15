package com.yral.shared.features.wallet.data

import com.yral.shared.features.wallet.data.models.toBtcInInr
import com.yral.shared.features.wallet.domain.models.BtcInInr
import com.yral.shared.features.wallet.domain.repository.WalletRepository

class WalletRepositoryImpl(
    private val dataSource: WalletDataSource,
) : WalletRepository {
    override suspend fun getBtcInInr(idToken: String): BtcInInr =
        dataSource
            .getBtcInInr(idToken)
            .toBtcInInr()

    override suspend fun getUserBtcBalance(userPrincipal: String): String =
        dataSource
            .getUserBtcBalance(userPrincipal)
}
