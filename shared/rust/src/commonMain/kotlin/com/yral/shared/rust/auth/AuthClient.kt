package com.yral.shared.rust.auth

import com.yral.shared.uniffi.generated.Principal
import io.ktor.client.plugins.cookies.HttpCookies

interface AuthClient {
    var identity: ByteArray?
    var canisterPrincipal: Principal?
    var userPrincipal: Principal?

    suspend fun initialize()
    suspend fun refreshAuthIfNeeded(cookie: HttpCookies)
    suspend fun generateNewDelegatedIdentity(): ByteArray
    suspend fun generateNewDelegatedIdentityWireOneHour(): ByteArray
}
