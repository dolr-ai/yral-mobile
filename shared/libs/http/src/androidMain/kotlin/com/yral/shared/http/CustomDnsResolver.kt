package com.yral.shared.http

import com.yral.shared.http.exception.DNSLookupException
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException

class CustomDnsResolver(
    client: OkHttpClient,
    private val httpEventListener: HTTPEventListener,
) : Dns {
    private val systemDns = Dns.SYSTEM
    private val dnsOverHttps: DnsOverHttps by lazy {
        DnsOverHttps
            .Builder()
            .client(client)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
            ).build()
    }

    @Suppress("SwallowedException")
    override fun lookup(hostname: String): List<InetAddress> =
        try {
            // Try system DNS first
            systemDns.lookup(hostname)
        } catch (e: UnknownHostException) {
            httpEventListener.logException(DNSLookupException("SystemDNS failed", e))
            try {
                // Fallback to DNS over HTTPS
                dnsOverHttps.lookup(hostname)
            } catch (fallbackException: UnknownHostException) {
                httpEventListener.logException(DNSLookupException("DNSOverHttp failed", e))
                emptyList()
            }
        }
}
