package com.yral.shared.features.auth

import kotlinx.coroutines.CoroutineScope

interface AuthClientFactory {
    fun create(
        scope: CoroutineScope,
        onAuthError: (YralAuthException) -> Unit,
    ): AuthClient
}
