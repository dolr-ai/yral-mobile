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

    suspend fun updateUserMetadata(
        identityData: ByteArray,
        userCanisterId: String,
        userName: String,
    ): Result<Unit, MetadataUpdateError>

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

sealed class MetadataUpdateError : Exception() {
    data class InvalidIdentityData(
        override val message: String,
    ) : MetadataUpdateError()
    data class InvalidCanisterId(
        override val message: String,
    ) : MetadataUpdateError()
    data class InvalidUsername(
        override val message: String,
    ) : MetadataUpdateError()
    data class UsernameTaken(
        override val message: String,
    ) : MetadataUpdateError()
    data class UnknownError(
        override val message: String,
    ) : MetadataUpdateError()
}

internal fun validateMetadataInputs(
    identityData: ByteArray,
    userCanisterId: String,
    userName: String,
): Result<Unit, MetadataUpdateError> =
    when {
        identityData.isEmpty() ->
            Err(
                MetadataUpdateError.InvalidIdentityData("Identity data cannot be empty"),
            )
        userCanisterId.isBlank() ->
            Err(
                MetadataUpdateError.InvalidCanisterId("Canister id cannot be blank"),
            )
        userName.isBlank() ->
            Err(
                MetadataUpdateError.InvalidUsername("Username cannot be blank"),
            )
        else -> Ok(Unit)
    }
