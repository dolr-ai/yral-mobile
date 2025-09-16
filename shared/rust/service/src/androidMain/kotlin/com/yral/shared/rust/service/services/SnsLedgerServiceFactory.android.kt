package com.yral.shared.rust.service.services

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.SnsLedgerService

actual class SnsLedgerServiceFactory {
    private var identityData: ByteArray? = null

    @Suppress("UnusedParameter")
    internal fun service(principal: Principal): SnsLedgerService =
        identityData?.let {
            SnsLedgerService(
                principalText = ICP_LEDGER_CANISTER,
                agentUrl = "",
            )
        } ?: throw YralException("Identity data not available")

    fun initialize(identityData: ByteArray) {
        this.identityData = identityData
    }

    companion object {
        const val ICP_LEDGER_CANISTER = "ryjl3-tyaaa-aaaaa-aaaba-cai"
    }
}
