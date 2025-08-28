package com.yral.shared.rust.service.services

actual object HelperService {
    actual suspend fun registerDevice(
        identityData: ByteArray,
        token: String,
    ) {
        com.yral.shared.uniffi.generated
            .registerDevice(identityData, token)
    }

    actual suspend fun unregisterDevice(
        identityData: ByteArray,
        token: String,
    ) {
        com.yral.shared.uniffi.generated
            .unregisterDevice(identityData, token)
    }
}
