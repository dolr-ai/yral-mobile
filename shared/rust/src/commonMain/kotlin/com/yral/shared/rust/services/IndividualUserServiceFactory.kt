package com.yral.shared.rust.services

import com.yral.shared.uniffi.generated.IndividualUserService
import com.yral.shared.uniffi.generated.Principal

class IndividualUserServiceFactory {
    private var service: IndividualUserService? = null

    fun service(): IndividualUserService = service ?: error("Service not initialised")

    fun initialize(
        principal: Principal,
        identityData: ByteArray,
    ) {
        service =
            IndividualUserService(
                principalText = principal,
                identityData = identityData,
            )
    }
}
