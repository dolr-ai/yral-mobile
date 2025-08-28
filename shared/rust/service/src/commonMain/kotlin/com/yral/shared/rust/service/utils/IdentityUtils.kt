package com.yral.shared.rust.service.utils

expect fun delegatedIdentityWireToJson(bytes: ByteArray): String

expect fun propicFromPrincipal(principalId: String): String

expect suspend fun authenticateWithNetwork(data: ByteArray): CanisterData

expect fun yralAuthLoginHint(identity: ByteArray): String
