package com.yral.shared.rust.services

import com.yral.shared.uniffi.generated.IndividualUserService
import com.yral.shared.uniffi.generated.Principal

class IndividualUserServiceFactory {
    private var principal: Principal? = null
    private var identityData: ByteArray? = null

    fun service(principal: Principal): IndividualUserService =
        identityData?.let {
            IndividualUserService(
                principalText = principal,
                identityData = it,
            )
        } ?: error("Identity data not available")

    fun initialize(
        principal: Principal,
        identityData: ByteArray,
    ) {
        this.principal = principal
        this.identityData = identityData
    }
}
