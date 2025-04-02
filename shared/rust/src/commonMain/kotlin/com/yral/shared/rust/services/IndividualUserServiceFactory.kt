package com.yral.shared.rust.services

import com.yral.shared.uniffi.generated.IndividualUserService
import com.yral.shared.uniffi.generated.Principal
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject

class IndividualUserServiceFactory {
    private var service: IndividualUserService? = null

    fun service(): IndividualUserService = service ?: throw IllegalStateException("Service not initialised")

    fun initialize(principal: Principal, identityData: ByteArray) {
        service = IndividualUserService(
            principal = principal,
            identityData = identityData,
        )
    }

    @OptIn(InternalCoroutinesApi::class)
    companion object : SynchronizedObject() {
        @Volatile
        private var instance: IndividualUserServiceFactory? = null

        fun getInstance(): IndividualUserServiceFactory =
            instance ?: synchronized(this) {
                instance ?: IndividualUserServiceFactory().also { instance = it }
            }
    }
}