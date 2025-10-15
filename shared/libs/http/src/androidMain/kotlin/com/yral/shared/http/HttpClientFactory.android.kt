package com.yral.shared.http

import android.app.Application
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.Cache
import java.io.File

private const val CACHE_SIZE = 10L * 1024 * 1024
private const val CACHE_FILE = "okhttpcache"

actual fun platformEngineFactory(): HttpClientEngineFactory<*> = OkHttp

@Suppress("UNCHECKED_CAST")
actual fun platformApplyEngineConfig(
    context: Any,
    config: HttpClientConfig<*>,
    httpEventListener: HTTPEventListener,
) {
    val okConfig = config as HttpClientConfig<OkHttpConfig>
    val appContext = context as Application
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
            val bootstrap =
                this
                    .cache(
                        Cache(
                            directory = File(appContext.cacheDir, CACHE_FILE),
                            maxSize = CACHE_SIZE,
                        ),
                    ).build()
            cache(null)
            dns(CustomDnsResolver(bootstrap, httpEventListener))
            addInterceptor(chuckerInterceptor)
        }
    }
}
