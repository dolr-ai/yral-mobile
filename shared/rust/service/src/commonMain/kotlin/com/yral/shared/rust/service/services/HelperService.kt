package com.yral.shared.rust.service.services

expect object HelperService {
    suspend fun registerDevice(
        identityData: ByteArray,
        token: String,
    )
    suspend fun unregisterDevice(
        identityData: ByteArray,
        token: String,
    )
}
