package com.yral.shared.features.auth

import com.yral.shared.core.exceptions.YralException

class YralAuthException(
    val error: String,
) : YralException(error) {
    constructor(e: Throwable) : this(e.message ?: "Unknown error")
}

class YralFBAuthException(
    val error: String,
) : YralException(error)
