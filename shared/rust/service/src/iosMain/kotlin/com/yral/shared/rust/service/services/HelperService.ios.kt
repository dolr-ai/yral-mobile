package com.yral.shared.rust.service.services

actual object HelperService {
    actual suspend fun registerDevice(
        identityData: ByteArray,
        token: String,
    ) {
        TODO("Not yet implemented")
    }

    actual suspend fun unregisterDevice(
        identityData: ByteArray,
        token: String,
    ) {
        TODO("Not yet implemented")
    }
}
