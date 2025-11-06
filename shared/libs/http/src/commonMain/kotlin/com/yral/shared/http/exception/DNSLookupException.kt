package com.yral.shared.http.exception

import com.yral.shared.core.exceptions.YralException

class DNSLookupException(
    override val message: String,
    override val cause: Throwable,
) : YralException("DNS lookup error: $message", cause)
