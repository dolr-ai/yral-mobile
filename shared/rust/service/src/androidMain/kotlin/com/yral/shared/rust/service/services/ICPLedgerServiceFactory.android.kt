package com.yral.shared.rust.service.services

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.uniffi.generated.LedgerService
import com.yral.shared.uniffi.generated.Principal

actual class ICPLedgerServiceFactory {
    private var identityData: ByteArray? = null

    internal fun service(principal: Principal): LedgerService =
        identityData?.let {
            LedgerService(
                principalText = principal,
                agentUrl = "",
            )
        } ?: throw YralException("Identity data not available")

    fun initialize(identityData: ByteArray) {
        this.identityData = identityData
    }
}
