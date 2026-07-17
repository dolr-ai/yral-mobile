package com.yral.shared.http

import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig

actual fun platformEngineFactory(): HttpClientEngineFactory<*> = OkHttp

actual fun platformApplyEngineConfig(
    context: Any,
    config: HttpClientConfig<*>,
    httpEventListener: HTTPEventListener,
) {
    val okConfig = config as HttpClientConfig<OkHttpConfig>
    val chuckerCollector =
        ChuckerCollector(
            context = context,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_HOUR,
        )
    val chuckerInterceptor =
        ChuckerInterceptor
            .Builder(context)
            .collector(chuckerCollector)
            .redactHeaders("Authorization", "Bearer")
            .alwaysReadResponseBody(false)
            .build()
    okConfig.engine {
        config {
            // Use system DNS (Dns.SYSTEM) — OkHttp default.
            // The previous CustomDnsResolver with DoH fallbacks (Google, Cloudflare, Quad9)
            // was causing 20s+ hangs when Jio blocks DoH endpoints, and was reporting
            // dns_lookup_failure errors in Crashlytics even when system DNS would have worked.
            // The OS's built-in DNS resolution handles retries and fallbacks natively.
            addInterceptor(chuckerInterceptor)
        }
    }
}
