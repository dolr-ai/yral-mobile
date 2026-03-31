package com.yral.shared.features.auth.data

import com.yral.shared.http.exception.DNSLookupException

internal fun Throwable.isDnsResolutionFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current.isPlatformDnsResolutionFailure()) {
            return true
        }
        current = current.cause
    }
    return false
}

internal expect fun Throwable.isPlatformDnsResolutionFailure(): Boolean

internal expect fun Throwable.toDnsLookupException(hostname: String): DNSLookupException

internal expect fun platformReportsDnsLookupFailure(): Boolean
