package com.yral.shared.http.exception

class DNSLookupException(
    override val message: String,
    override val cause: Throwable,
) : Exception("DNS lookup error: $message", cause)
