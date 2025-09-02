package com.yral.shared.rust.service.utils

actual fun delegatedIdentityWireToJson(bytes: ByteArray): String = ""
actual fun propicFromPrincipal(principalId: String): String = ""
actual suspend fun authenticateWithNetwork(data: ByteArray): CanisterData {
    TODO("Not yet implemented")
}

actual fun yralAuthLoginHint(identity: ByteArray): String = ""
