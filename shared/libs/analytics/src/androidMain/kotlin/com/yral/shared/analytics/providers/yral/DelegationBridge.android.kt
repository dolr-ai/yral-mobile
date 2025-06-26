package com.yral.shared.analytics.providers.yral

actual fun delegatedIdentityWireToJson(bytes: ByteArray): String =
    com.yral.shared.uniffi.generated
        .delegatedIdentityWireToJson(bytes)
