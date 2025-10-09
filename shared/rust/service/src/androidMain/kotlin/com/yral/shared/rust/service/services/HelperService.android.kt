package com.yral.shared.rust.service.services

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.koin.koinInstance

actual object HelperService {
    private val logger = Logger.withTag("HelperService")

    actual suspend fun registerDevice(
        identityData: ByteArray,
        token: String,
    ): Result<Unit, DeviceRegistrationError> =
        try {
            // Validate inputs
            validateDeviceRegistrationInputs(identityData, token).getOrThrow()

            logger.d { "Registering device with token: $token" }

            // Call the uniffi generated function
            com.yral.shared.uniffi.generated
                .registerDevice(identityData, token)

            Ok(Unit)
        } catch (e: DeviceRegistrationError) {
            Err(e)
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            logger.e(e) { "Unexpected error during device registration" }
            Err(DeviceRegistrationError.UnknownError(e.message ?: "Unexpected error occurred"))
        }

    actual suspend fun unregisterDevice(
        identityData: ByteArray,
        token: String,
    ): Result<Unit, DeviceRegistrationError> =
        try {
            // Validate inputs
            validateDeviceRegistrationInputs(identityData, token).getOrThrow()

            logger.d { "Unregistering device with token: $token" }

            // Call the uniffi generated function
            com.yral.shared.uniffi.generated
                .unregisterDevice(identityData, token)

            Ok(Unit)
        } catch (e: DeviceRegistrationError) {
            Err(e)
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            logger.e(e) { "Unexpected error during device unregistration" }
            Err(DeviceRegistrationError.UnknownError(e.message ?: "Unexpected error occurred"))
        }

    actual suspend fun updateUserMetadata(
        identityData: ByteArray,
        userCanisterId: String,
        userName: String,
    ): Result<Unit, MetadataUpdateError> =
        try {
            validateMetadataInputs(identityData, userCanisterId, userName).getOrThrow()

            logger.d { "Updating metadata for canister: $userCanisterId" }

            com.yral.shared.uniffi.generated
                .setUserMetadata(identityData, userCanisterId, userName)

            Ok(Unit)
        } catch (e: MetadataUpdateError) {
            Err(e)
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            val errorMessage = e.message.orEmpty()
            logger.e(e) { "Unexpected error during metadata update" }
            val mappedError =
                if (errorMessage.contains("DuplicateUsername", ignoreCase = true)) {
                    MetadataUpdateError.UsernameTaken("This username is already taken.")
                } else {
                    MetadataUpdateError.UnknownError(errorMessage.ifBlank { "Unexpected error occurred" })
                }
            Err(mappedError)
        }

    actual fun initRustLogger() {
        com.yral.shared.uniffi.generated
            .initRustLogger()
    }

    actual fun initServiceFactories(identityData: ByteArray) {
        koinInstance.get<IndividualUserServiceFactory>().initialize(identityData)
        koinInstance.get<RateLimitServiceFactory>().initialize(identityData)
        koinInstance.get<UserPostServiceFactory>().initialize(identityData)
        koinInstance.get<SnsLedgerServiceFactory>().initialize(identityData)
        koinInstance.get<ICPLedgerServiceFactory>().initialize(identityData)
    }
}
