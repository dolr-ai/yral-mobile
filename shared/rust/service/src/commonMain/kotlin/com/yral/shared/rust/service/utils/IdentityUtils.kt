package com.yral.shared.rust.service.utils

import com.yral.shared.uniffi.generated.FfiException

fun delegatedIdentityWireToJson(bytes: ByteArray): String =
    com.yral.shared.uniffi.generated
        .delegatedIdentityWireToJson(bytes)

fun propicFromPrincipal(principalId: String): String =
    com.yral.shared.uniffi.generated
        .propicFromPrincipal(principalId)

suspend fun authenticateWithNetwork(data: ByteArray): CanisterData {
    try {
        val wrapper =
            com.yral.shared.uniffi.generated
                .authenticateWithNetwork(data)
        return CanisterData(
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
