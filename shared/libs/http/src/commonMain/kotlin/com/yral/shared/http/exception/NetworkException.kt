package com.yral.shared.http.exception

class NetworkException(
    override val cause: Throwable,
) : Exception("Network error", cause)
