package com.yral.shared.rust.service.services

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.koin.koinInstance
import com.yral.shared.uniffi.generated.LogLevel
import com.yral.shared.uniffi.generated.LoggerException

object HelperService {
    private val logger = Logger.withTag("HelperService")

    suspend fun registerDevice(
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

    suspend fun unregisterDevice(
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

    suspend fun updateUserMetadata(
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

    fun initRustLogger() {
        try {
            com.yral.shared.uniffi.generated
                .initRustLogger(
                    customTag = "YralMobileRust",
                    maxLevel = LogLevel.DEBUG,
                )
            startLogForwarding()
            logger.i { "Rust logger initialized successfully" }
        } catch (e: LoggerException) {
            logger.e { "Failed to initialize Rust logger: $e" }
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            logger.e(e) { "Unexpected error initializing Rust logger" }
        }
    }

    fun startLogForwarding() {
        val logForwardingService = koinInstance.get<LogForwardingService>()
        logForwardingService.startForwarding()
        logger.i { "Log forwarding service started" }
    }

    fun initServiceFactories(identityData: ByteArray) {
        koinInstance.get<IndividualUserServiceFactory>().initialize(identityData)
        koinInstance.get<RateLimitServiceFactory>().initialize(identityData)
        koinInstance.get<UserPostServiceFactory>().initialize(identityData)
        koinInstance.get<UserInfoServiceFactory>().initialize(identityData)
        koinInstance.get<SnsLedgerServiceFactory>().initialize(identityData)
        koinInstance.get<ICPLedgerServiceFactory>().initialize(identityData)
    }
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
