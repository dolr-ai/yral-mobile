package com.yral.shared.http

import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingFormat
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal const val TIME_OUT = 30000L
const val UPLOAD_FILE_TIME_OUT = 5 * 60 * 1000L // 5 min

expect fun platformEngineFactory(): HttpClientEngineFactory<*>
expect fun platformApplyEngineConfig(
    context: Any,
    config: HttpClientConfig<*>,
    httpEventListener: HTTPEventListener,
)

fun createClient(
    context: Any,
    httpEventListener: HTTPEventListener,
    preferences: Preferences,
    json: Json,
    httpLogger: HttpLogger,
): HttpClient =
    HttpClient(platformEngineFactory()) {
        platformApplyEngineConfig(context, this, httpEventListener)
        install(HttpTimeout) {
            requestTimeoutMillis = TIME_OUT
            socketTimeoutMillis = TIME_OUT
        }
        install(Logging) {
            format = LoggingFormat.OkHttp
            logger = httpLogger
            level = httpLogger.logLevel
            filter {
                it.body !is OutgoingContent
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpCookies) {
            storage = PersistentCookieStorage(preferences)
        }
        expectSuccess = true
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
            }
            contentType(ContentType.Application.Json)
        }
    }
