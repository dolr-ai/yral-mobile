package com.yral.shared.http

import com.yral.shared.http.exception.DNSLookupException
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val DEVICE_DNS = "device_dns"
private const val GOOGLE_DOH = "google_doh"
private const val CLOUDFLARE_DOH = "cloudflare_doh"
private const val QUAD9_DOH = "quad9_doh"
private const val DOH_TIMEOUT_SECONDS = 5L

class CustomDnsResolver internal constructor(
    private val resolvers: List<NamedDnsResolver>,
    private val httpEventListener: HTTPEventListener,
) : Dns {
    constructor(
        client: OkHttpClient,
        httpEventListener: HTTPEventListener,
    ) : this(createResolvers(client), httpEventListener)

    override fun lookup(hostname: String): List<InetAddress> {
        val failures = mutableListOf<Throwable>()
        resolvers.forEach { resolver ->
            try {
                return resolver.dns.lookup(hostname)
            } catch (e: UnknownHostException) {
                failures += e
            }
        }

        val finalFailure = failures.lastOrNull() as? UnknownHostException ?: UnknownHostException(hostname)
        failures.dropLast(1).forEach(finalFailure::addSuppressed)
        httpEventListener.logException(
            DNSLookupException(
                hostname = hostname,
                lookupSource = resolvers.joinToString(separator = "_") { it.name },
                cause = finalFailure,
            ),
        )
        throw finalFailure
    }

    companion object {
        private fun createResolvers(client: OkHttpClient): List<NamedDnsResolver> {
            val dohClient =
                client
                    .newBuilder()
                    .callTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .connectTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()
            return listOf(
                NamedDnsResolver(DEVICE_DNS, Dns.SYSTEM),
                NamedDnsResolver(
                    GOOGLE_DOH,
                    dohClient.dnsOverHttps(
                        url = "https://dns.google/dns-query",
                        primaryBootstrapHost = "8.8.8.8",
                        secondaryBootstrapHost = "8.8.4.4",
                    ),
                ),
                NamedDnsResolver(
                    CLOUDFLARE_DOH,
                    dohClient.dnsOverHttps(
                        url = "https://cloudflare-dns.com/dns-query",
                        primaryBootstrapHost = "1.1.1.1",
                        secondaryBootstrapHost = "1.0.0.1",
                    ),
                ),
                NamedDnsResolver(
                    QUAD9_DOH,
                    dohClient.dnsOverHttps(
                        url = "https://dns.quad9.net/dns-query",
                        primaryBootstrapHost = "9.9.9.9",
                        secondaryBootstrapHost = "149.112.112.112",
                    ),
                ),
            )
        }
    }
}

internal data class NamedDnsResolver(
    val name: String,
    val dns: Dns,
)

private fun OkHttpClient.dnsOverHttps(
    url: String,
    primaryBootstrapHost: String,
    secondaryBootstrapHost: String,
): DnsOverHttps =
    DnsOverHttps
        .Builder()
        .client(this)
        .url(url.toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName(primaryBootstrapHost),
            InetAddress.getByName(secondaryBootstrapHost),
        ).build()
