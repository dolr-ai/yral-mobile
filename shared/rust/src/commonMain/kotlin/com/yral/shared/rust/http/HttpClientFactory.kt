package com.yral.shared.rust.http

import com.yral.shared.preferences.PrefUtils
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

const val BaseURL = "yral.com"

object HttpClientFactory {
    var client: HttpClient = createClient()

    fun recreateHttpClient() {
        client = createClient()
    }

    private fun createClient(): HttpClient {
        return HttpClient(CIO) {
            install(Logging) {
                logger = object : Logger {
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
                storage = PersistentCookieStorage(PrefUtils())
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = BaseURL
                }
                contentType(ContentType.Application.Json)
            }
        }
    }
}
