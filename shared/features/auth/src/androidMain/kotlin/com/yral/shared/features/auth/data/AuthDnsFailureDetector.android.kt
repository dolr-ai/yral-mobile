package com.yral.shared.features.auth.data

import com.yral.shared.http.exception.DNSLookupException
import java.net.UnknownHostException

internal actual fun Throwable.isPlatformDnsResolutionFailure(): Boolean =
    this is DNSLookupException ||
        this is UnknownHostException

internal actual fun Throwable.toDnsLookupException(hostname: String): DNSLookupException =
    when (this) {
        is DNSLookupException -> {
            this
        }

        else -> {
            DNSLookupException(
                hostname = hostname,
                lookupSource = "unknown_host",
                cause = this,
            )
        }
    }

internal actual fun platformReportsDnsLookupFailure(): Boolean = true
