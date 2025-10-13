package com.yral.shared.rust.service.services

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.uniffi.generated.IndividualUserService
import com.yral.shared.uniffi.generated.Principal

class IndividualUserServiceFactory {
    private var identityData: ByteArray? = null

    internal fun service(principal: Principal): IndividualUserService =
        identityData?.let {
            IndividualUserService(
                principalText = principal,
                identityData = it,
            )
        } ?: throw YralException("Identity data not available")

    fun initialize(identityData: ByteArray) {
        this.identityData = identityData
    }
}
