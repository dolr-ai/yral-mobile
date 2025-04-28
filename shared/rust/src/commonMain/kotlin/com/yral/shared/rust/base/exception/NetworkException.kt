package com.yral.shared.rust.base.exception

class NetworkException(
    override val cause: Throwable,
) : Exception("Network error", cause)
