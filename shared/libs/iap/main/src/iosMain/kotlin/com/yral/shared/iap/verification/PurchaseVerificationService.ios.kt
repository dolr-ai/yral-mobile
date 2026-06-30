package com.yral.shared.iap.verification

internal actual fun getVerifierEndPoint(): String = "apple/chat-access/grant"

internal actual fun supportsAppleAppAccountToken(): Boolean = true
