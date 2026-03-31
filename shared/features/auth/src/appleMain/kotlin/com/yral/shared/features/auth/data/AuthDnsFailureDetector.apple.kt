package com.yral.shared.features.auth.data

import com.yral.shared.http.exception.DNSLookupException
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain

internal actual fun Throwable.isPlatformDnsResolutionFailure(): Boolean =
    when (this) {
        is DNSLookupException -> true
        is DarwinHttpRequestException ->
            origin.domain == NSURLErrorDomain &&
                (origin.code == NSURLErrorCannotFindHost || origin.code == NSURLErrorDNSLookupFailed)
        else -> false
    }

internal actual fun Throwable.toDnsLookupException(hostname: String): DNSLookupException =
    when (this) {
        is DNSLookupException -> this
        is DarwinHttpRequestException ->
            DNSLookupException(
                hostname = hostname,
                lookupSource =
                    when (origin.code) {
                        NSURLErrorCannotFindHost -> "darwin_cannot_find_host"
                        NSURLErrorDNSLookupFailed -> "darwin_dns_lookup_failed"
                        else -> "darwin_request"
                    },
                cause = this,
            )
        else ->
            DNSLookupException(
                hostname = hostname,
                lookupSource = "darwin_request",
                cause = this,
            )
    }

internal actual fun platformReportsDnsLookupFailure(): Boolean = false
