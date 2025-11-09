package com.yral.shared.features.auth.di

import org.koin.core.scope.Scope

internal actual fun Scope.createAuthEnv(): AuthEnv =
    AuthEnv(
        clientId = "c89b29de-8366-4e62-9b9e-c29585740acf",
        redirectUri = AuthEnv.RedirectUri(scheme = "yral"),
    )
