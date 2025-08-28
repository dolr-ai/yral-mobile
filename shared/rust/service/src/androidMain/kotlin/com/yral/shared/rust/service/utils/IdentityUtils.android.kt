package com.yral.shared.rust.service.utils

import com.yral.shared.uniffi.generated.FfiException

actual fun delegatedIdentityWireToJson(bytes: ByteArray): String =
    com.yral.shared.uniffi.generated
        .delegatedIdentityWireToJson(bytes)

actual fun propicFromPrincipal(principalId: String): String =
    com.yral.shared.uniffi.generated
        .propicFromPrincipal(principalId)

actual suspend fun authenticateWithNetwork(data: ByteArray): CanisterData {
    try {
        val wrapper =
            com.yral.shared.uniffi.generated
                .authenticateWithNetwork(data, null)
        return CanisterData(
            canisterId = wrapper.getCanisterPrincipal(),
            userPrincipalId = wrapper.getUserPrincipal(),
        )
    } catch (exception: FfiException) {
        throw YralFfiException(exception)
    }
}

actual fun yralAuthLoginHint(identity: ByteArray): String =
    com.yral.shared.uniffi.generated
        .yralAuthLoginHint(identity)
