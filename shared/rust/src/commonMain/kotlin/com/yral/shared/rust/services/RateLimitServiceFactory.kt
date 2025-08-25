package com.yral.shared.rust.services

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.RateLimitService

class RateLimitServiceFactory {
    private var identityData: ByteArray? = null

    fun service(principal: Principal): RateLimitService =
        identityData?.let {
            RateLimitService(
                principalText = principal,
                identityData = it,
            )
        } ?: throw YralException("Identity data not available")

    fun initialize(identityData: ByteArray) {
        this.identityData = identityData
    }
}
