package com.yral.shared.rust.service.services

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

expect object HelperService {
    suspend fun registerDevice(
        identityData: ByteArray,
        token: String,
    ): Result<Unit, DeviceRegistrationError>

    suspend fun unregisterDevice(
        identityData: ByteArray,
        token: String,
    ): Result<Unit, DeviceRegistrationError>

    fun initRustLogger()

    fun initServiceFactories(identityData: ByteArray)
}

sealed class DeviceRegistrationError : Exception() {
    data class InvalidIdentityData(
        override val message: String,
    ) : DeviceRegistrationError()
    data class InvalidToken(
        override val message: String,
    ) : DeviceRegistrationError()
    data class UnknownError(
        override val message: String,
    ) : DeviceRegistrationError()
}

internal fun validateDeviceRegistrationInputs(
    identityData: ByteArray,
    token: String,
): Result<Unit, DeviceRegistrationError> =
    when {
        identityData.isEmpty() ->
            Err(
                DeviceRegistrationError.InvalidIdentityData("Identity data cannot be empty"),
            )
        token.isBlank() ->
            Err(
                DeviceRegistrationError.InvalidToken("Token cannot be blank"),
            )
        else -> Ok(Unit)
    }
