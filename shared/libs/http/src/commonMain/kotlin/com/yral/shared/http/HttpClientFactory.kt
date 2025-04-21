package com.yral.shared.http

import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HttpClientFactory(
    private val preferences: Preferences,
) {
    fun createClient(baseUrl: String? = BASE_URL): HttpClient =
        HttpClient(CIO) {
            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            println("HTTP Client $message")
                        }
                    }
                level = LogLevel.BODY
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
            install(HttpCookies) {
                storage = PersistentCookieStorage(preferences)
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = baseUrl ?: BASE_URL
                }
                contentType(ContentType.Application.Json)
            }
        }

    companion object {
        private const val BASE_URL = "https://icp-off-chain-agent.fly.dev"
    }
}
