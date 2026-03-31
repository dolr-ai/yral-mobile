package com.yral.shared.features.auth.data

import com.yral.shared.rust.service.utils.yralAuthLoginHint

interface AuthLoginHintProvider {
    fun build(identity: ByteArray): String
}

class DefaultAuthLoginHintProvider : AuthLoginHintProvider {
    override fun build(identity: ByteArray): String = yralAuthLoginHint(identity)
}
