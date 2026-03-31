package com.yral.shared.http.exception

import com.yral.shared.core.exceptions.YralException

class DNSLookupException(
    val hostname: String,
    val lookupSource: String,
    override val cause: Throwable,
) : YralException("DNS lookup error for host=$hostname source=$lookupSource", cause)
