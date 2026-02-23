package com.yral.shared.rust.service.utils

import com.yral.shared.koin.koinInstance
import com.yral.shared.rust.service.domain.performance.traceApiCall
import com.yral.shared.uniffi.generated.FfiException
import com.yral.shared.uniffi.generated.ServiceCanistersDetails
import kotlin.time.Duration.Companion.seconds

fun delegatedIdentityWireToJson(bytes: ByteArray): String =
    com.yral.shared.uniffi.generated
        .delegatedIdentityWireToJson(bytes)

fun propicFromPrincipal(principalId: String): String =
    com.yral.shared.uniffi.generated
        .propicFromPrincipal(principalId)

/**
 * Gets session data from identity without making network calls.
 * Uses Identity.sender() to extract principal and USER_INFO_SERVICE_ID as default canister.
 */
fun getSessionFromIdentity(data: ByteArray): CanisterData {
    try {
        val userPrincipal =
            com.yral.shared.uniffi.generated
                .getPrincipalFromIdentity(data)
        val serviceCanisterDetails = ServiceCanistersDetails()
        val canisterId = serviceCanisterDetails.getUserInfoServiceCanisterId()

        return CanisterData(
            canisterId = canisterId,
            userPrincipalId = userPrincipal,
            profilePic = propicFromPrincipal(userPrincipal),
            username = null,
            isCreatedFromServiceCanister = true,
        )
    } catch (exception: FfiException) {
        throw YralFfiException(exception)
    }
}

suspend fun authenticateWithNetwork(data: ByteArray): CanisterData =
    traceApiCall(koinInstance.get(), "authenticateWithNetwork") {
        try {
            val wrapper =
                com.yral.shared.uniffi.generated
                    .authenticateWithNetwork(data)
            CanisterData(
                canisterId = wrapper.getCanisterPrincipal(),
                userPrincipalId = wrapper.getUserPrincipal(),
                profilePic = wrapper.getProfilePic(),
                username = wrapper.getUsername(),
                isCreatedFromServiceCanister = wrapper.isCreatedFromServiceCanister(),
            )
        } catch (exception: FfiException) {
            throw YralFfiException(exception)
        }
    }

fun yralAuthLoginHint(identity: ByteArray): String =
    com.yral.shared.uniffi.generated
        .yralAuthLoginHint(identity)

data class SignedMessage(
    val sig: ByteArray?,
    val publicKey: ByteArray?,
    val ingressExpirySecs: Long,
    val ingressExpiryNanos: Int,
    val delegations: List<SignedDelegationPayload>?,
    val sender: String,
)

data class DelegationPayload(
    val pubkey: ByteArray,
    val expiration: Long,
    val targets: List<String>?,
)

data class SignedDelegationPayload(
    val delegation: DelegationPayload,
    val signature: ByteArray,
)

fun signMessageWithIdentity(
    identity: ByteArray,
    message: String,
): SignedMessage {
    val result =
        com.yral.shared.uniffi.generated
            .signMessageInternal(identity, message)
    val signatureBytes = result.sig?.map { it.toByte() }?.toByteArray()
    val publicKeyBytes = result.publicKey?.map { it.toByte() }?.toByteArray()
    val delegations =
        result.delegations?.map { del ->
            SignedDelegationPayload(
                delegation =
                    DelegationPayload(
                        pubkey =
                            del.delegation.pubkey
                                .map { it.toByte() }
                                .toByteArray(),
                        expiration = del.delegation.expiration.toLong(),
                        targets = del.delegation.targets,
                    ),
                signature = del.signature.map { it.toByte() }.toByteArray(),
            )
        }
    val ingressExpiryDuration = result.ingressExpiry
    val ingressExpirySeconds = ingressExpiryDuration.inWholeSeconds
    val ingressExpiryNanos =
        (ingressExpiryDuration - ingressExpirySeconds.seconds).inWholeNanoseconds.toInt()
    return SignedMessage(
        sig = signatureBytes,
        publicKey = publicKeyBytes,
        ingressExpirySecs = ingressExpirySeconds,
        ingressExpiryNanos = ingressExpiryNanos,
        delegations = delegations,
        sender = result.sender,
    )
}
