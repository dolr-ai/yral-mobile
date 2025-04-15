package com.yral.shared.features.auth

import com.yral.shared.uniffi.generated.Principal

interface AuthClient {
    var identity: ByteArray?
    var canisterPrincipal: Principal?
    var userPrincipal: Principal?

    suspend fun initialize()
    suspend fun refreshAuthIfNeeded()
    suspend fun generateNewDelegatedIdentity(): ByteArray
    suspend fun generateNewDelegatedIdentityWireOneHour(): ByteArray
}
