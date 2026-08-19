package com.yral.shared.features.auth.data

interface AuthLoginHintProvider {
    fun build(): String
}

class DefaultAuthLoginHintProvider : AuthLoginHintProvider {
    // yralAuthLoginHint (Rust FFI) removed — login hint is no longer derived from
    // the identity bytes. The backend JWT flow does not require a login hint.
    override fun build(): String = ""
}
