package com.yral.shared.features.auth

import com.yral.shared.core.exceptions.YralException

class YralAuthException : YralException {
    val error: String

    constructor(error: String) : super(error) {
        this.error = error
    }

    constructor(error: String, cause: Throwable) : super("Unknown exception: $error", cause) {
        this.error = error
    }

    constructor(e: Throwable) : this(e.message ?: "Unknown error")
}

class YralFBAuthException(
    val error: String,
) : YralException(error)
