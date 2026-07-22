package com.yral.shared.http.exception

class NetworkException(
    override val cause: Throwable,
) : Exception("Network error", cause)

fun Throwable.hasNetworkCause(): Boolean = generateSequence(this) { it.cause }.any { it is NetworkException }
