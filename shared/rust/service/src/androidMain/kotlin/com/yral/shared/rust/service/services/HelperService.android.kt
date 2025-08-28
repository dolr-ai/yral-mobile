package com.yral.shared.rust.service.services

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow

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
}
