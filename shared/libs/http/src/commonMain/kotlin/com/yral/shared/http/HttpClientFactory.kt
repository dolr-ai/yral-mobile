package com.yral.shared.http

import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createClient(
    preferences: Preferences,
    json: Json,
    consoleLogger: ConsoleLogger,
): HttpClient =
    HttpClient(CIO) {
        install(Logging) {
            logger = consoleLogger
            level = consoleLogger.logLevel
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpCookies) {
            storage = PersistentCookieStorage(preferences)
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
            }
            contentType(ContentType.Application.Json)
        }
    }
