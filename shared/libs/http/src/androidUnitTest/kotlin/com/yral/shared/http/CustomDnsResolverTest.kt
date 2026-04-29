package com.yral.shared.http

import com.yral.shared.http.exception.DNSLookupException
import okhttp3.Dns
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class CustomDnsResolverTest {
    private val host = "offchain.yral.com"
    private val resolvedAddress = InetAddress.getByName("192.0.2.10")

    @Test
    fun deviceDnsSuccessDoesNotCallFallbackResolvers() {
        val deviceDns = FakeDns(result = DnsResult.Success(listOf(resolvedAddress)))
        val googleDns = FakeDns(result = DnsResult.Failure("google failed"))
        val cloudflareDns = FakeDns(result = DnsResult.Failure("cloudflare failed"))
        val quad9Dns = FakeDns(result = DnsResult.Failure("quad9 failed"))
        val listener = RecordingHTTPEventListener()

        val result =
            createResolver(
                listener = listener,
                deviceDns = deviceDns,
                googleDns = googleDns,
                cloudflareDns = cloudflareDns,
                quad9Dns = quad9Dns,
            ).lookup(host)

        assertEquals(listOf(resolvedAddress), result)
        assertEquals(listOf(host), deviceDns.lookups)
        assertTrue(googleDns.lookups.isEmpty())
        assertTrue(cloudflareDns.lookups.isEmpty())
        assertTrue(quad9Dns.lookups.isEmpty())
        assertTrue(listener.exceptions.isEmpty())
    }

    @Test
    fun googleDnsIsUsedWhenDeviceDnsFails() {
        val deviceDns = FakeDns(result = DnsResult.Failure("device failed"))
        val googleDns = FakeDns(result = DnsResult.Success(listOf(resolvedAddress)))
        val cloudflareDns = FakeDns(result = DnsResult.Failure("cloudflare failed"))
        val quad9Dns = FakeDns(result = DnsResult.Failure("quad9 failed"))
        val listener = RecordingHTTPEventListener()

        val result =
            createResolver(
                listener = listener,
                deviceDns = deviceDns,
                googleDns = googleDns,
                cloudflareDns = cloudflareDns,
                quad9Dns = quad9Dns,
            ).lookup(host)

        assertEquals(listOf(resolvedAddress), result)
        assertEquals(listOf(host), deviceDns.lookups)
        assertEquals(listOf(host), googleDns.lookups)
        assertTrue(cloudflareDns.lookups.isEmpty())
        assertTrue(quad9Dns.lookups.isEmpty())
        assertTrue(listener.exceptions.isEmpty())
    }

    @Test
    fun cloudflareDnsIsUsedWhenDeviceAndGoogleDnsFail() {
        val deviceDns = FakeDns(result = DnsResult.Failure("device failed"))
        val googleDns = FakeDns(result = DnsResult.Failure("google failed"))
        val cloudflareDns = FakeDns(result = DnsResult.Success(listOf(resolvedAddress)))
        val quad9Dns = FakeDns(result = DnsResult.Failure("quad9 failed"))
        val listener = RecordingHTTPEventListener()

        val result =
            createResolver(
                listener = listener,
                deviceDns = deviceDns,
                googleDns = googleDns,
                cloudflareDns = cloudflareDns,
                quad9Dns = quad9Dns,
            ).lookup(host)

        assertEquals(listOf(resolvedAddress), result)
        assertEquals(listOf(host), deviceDns.lookups)
        assertEquals(listOf(host), googleDns.lookups)
        assertEquals(listOf(host), cloudflareDns.lookups)
        assertTrue(quad9Dns.lookups.isEmpty())
        assertTrue(listener.exceptions.isEmpty())
    }

    @Test
    fun quad9DnsIsUsedWhenEarlierResolversFail() {
        val deviceDns = FakeDns(result = DnsResult.Failure("device failed"))
        val googleDns = FakeDns(result = DnsResult.Failure("google failed"))
        val cloudflareDns = FakeDns(result = DnsResult.Failure("cloudflare failed"))
        val quad9Dns = FakeDns(result = DnsResult.Success(listOf(resolvedAddress)))
        val listener = RecordingHTTPEventListener()

        val result =
            createResolver(
                listener = listener,
                deviceDns = deviceDns,
                googleDns = googleDns,
                cloudflareDns = cloudflareDns,
                quad9Dns = quad9Dns,
            ).lookup(host)

        assertEquals(listOf(resolvedAddress), result)
        assertEquals(listOf(host), deviceDns.lookups)
        assertEquals(listOf(host), googleDns.lookups)
        assertEquals(listOf(host), cloudflareDns.lookups)
        assertEquals(listOf(host), quad9Dns.lookups)
        assertTrue(listener.exceptions.isEmpty())
    }

    @Test
    fun allResolverFailuresLogOneDnsLookupExceptionAndThrowFinalFailure() {
        val deviceFailure = unknownHost("device failed")
        val googleFailure = unknownHost("google failed")
        val cloudflareFailure = unknownHost("cloudflare failed")
        val quad9Failure = unknownHost("quad9 failed")
        val listener = RecordingHTTPEventListener()

        val thrown =
            try {
                createResolver(
                    listener = listener,
                    deviceDns = FakeDns(result = DnsResult.Failure(deviceFailure)),
                    googleDns = FakeDns(result = DnsResult.Failure(googleFailure)),
                    cloudflareDns = FakeDns(result = DnsResult.Failure(cloudflareFailure)),
                    quad9Dns = FakeDns(result = DnsResult.Failure(quad9Failure)),
                ).lookup(host)
                fail("Expected DNS lookup to fail")
            } catch (e: UnknownHostException) {
                e
            }

        assertSame(quad9Failure, thrown)
        assertEquals(listOf(deviceFailure, googleFailure, cloudflareFailure), thrown.suppressed.toList())
        assertEquals(1, listener.exceptions.size)
        val loggedException = assertIs<DNSLookupException>(listener.exceptions.single())
        assertEquals(host, loggedException.hostname)
        assertEquals("device_dns_google_doh_cloudflare_doh_quad9_doh", loggedException.lookupSource)
        assertSame(quad9Failure, loggedException.cause)
    }

    @Test
    fun timeoutBackedUnknownHostFailureContinuesToNextResolver() {
        val deviceFailure = unknownHost("device failed")
        val googleFailure =
            unknownHost("google timeout").apply {
                initCause(SocketTimeoutException("google timed out"))
            }
        val cloudflareDns = FakeDns(result = DnsResult.Success(listOf(resolvedAddress)))
        val listener = RecordingHTTPEventListener()

        val result =
            createResolver(
                listener = listener,
                deviceDns = FakeDns(result = DnsResult.Failure(deviceFailure)),
                googleDns = FakeDns(result = DnsResult.Failure(googleFailure)),
                cloudflareDns = cloudflareDns,
                quad9Dns = FakeDns(result = DnsResult.Failure("quad9 failed")),
            ).lookup(host)

        assertEquals(listOf(resolvedAddress), result)
        assertEquals(listOf(host), cloudflareDns.lookups)
        assertTrue(listener.exceptions.isEmpty())
    }

    private fun createResolver(
        listener: HTTPEventListener,
        deviceDns: Dns,
        googleDns: Dns,
        cloudflareDns: Dns,
        quad9Dns: Dns,
    ): CustomDnsResolver =
        CustomDnsResolver(
            resolvers =
                listOf(
                    NamedDnsResolver("device_dns", deviceDns),
                    NamedDnsResolver("google_doh", googleDns),
                    NamedDnsResolver("cloudflare_doh", cloudflareDns),
                    NamedDnsResolver("quad9_doh", quad9Dns),
                ),
            httpEventListener = listener,
        )

    private fun unknownHost(message: String): UnknownHostException = UnknownHostException(message)

    private sealed interface DnsResult {
        data class Success(
            val addresses: List<InetAddress>,
        ) : DnsResult

        data class Failure(
            val exception: UnknownHostException,
        ) : DnsResult {
            constructor(message: String) : this(UnknownHostException(message))
        }
    }

    private class FakeDns(
        private val result: DnsResult,
    ) : Dns {
        val lookups = mutableListOf<String>()

        override fun lookup(hostname: String): List<InetAddress> {
            lookups += hostname
            return when (result) {
                is DnsResult.Success -> result.addresses
                is DnsResult.Failure -> throw result.exception
            }
        }
    }

    private class RecordingHTTPEventListener : HTTPEventListener {
        val exceptions = mutableListOf<Exception>()

        override fun logException(e: Exception) {
            exceptions += e
        }
    }
}
