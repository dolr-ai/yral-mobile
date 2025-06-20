package com.yral.shared.http

import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TIME_OUT = 30000L

fun createClient(
    preferences: Preferences,
    json: Json,
    httpLogger: HttpLogger,
): HttpClient =
    HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = TIME_OUT
            socketTimeoutMillis = TIME_OUT
        }
        install(Logging) {
            logger = httpLogger
            level = httpLogger.logLevel
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
