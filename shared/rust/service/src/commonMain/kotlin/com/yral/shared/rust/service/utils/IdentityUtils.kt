package com.yral.shared.rust.service.utils

import com.yral.shared.koin.koinInstance
import com.yral.shared.rust.service.domain.performance.traceApiCall
import com.yral.shared.uniffi.generated.FfiException
import com.yral.shared.uniffi.generated.ServiceCanistersDetails

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
