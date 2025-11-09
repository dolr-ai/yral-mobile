package com.yral.shared.features.auth.di

import org.koin.core.scope.Scope
import platform.Foundation.NSBundle

internal actual fun Scope.createAuthEnv(): AuthEnv {
    val scheme =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("YRAL_REDIRECT_URI_SCHEME") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: error("YRAL_REDIRECT_URI_SCHEME missing from Info.plist")
    return AuthEnv(
        clientId = "e1a6a7fb-8a1d-42dc-87b4-13ff94ecbe34",
        redirectUri = AuthEnv.RedirectUri(scheme = scheme),
    )
}
