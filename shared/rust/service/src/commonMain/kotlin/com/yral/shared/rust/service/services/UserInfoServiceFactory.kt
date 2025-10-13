package com.yral.shared.rust.service.services

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UserInfoService

class UserInfoServiceFactory {
    private var identityData: ByteArray? = null

    internal fun service(principal: Principal): UserInfoService =
        identityData?.let {
            UserInfoService(
                principalText = principal,
                identityData = it,
            )
        } ?: throw YralException("Identity data not available")

    fun initialize(identityData: ByteArray) {
        this.identityData = identityData
    }
}
