package com.yral.shared.rust.service.services

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.SnsLedgerService

class SnsLedgerServiceFactory {
    private var identityData: ByteArray? = null

    internal fun service(principal: Principal): SnsLedgerService =
        identityData?.let {
            SnsLedgerService(
                principalText = principal,
                agentUrl = "",
            )
        } ?: throw YralException("Identity data not available")

    fun initialize(identityData: ByteArray) {
        this.identityData = identityData
    }
}
