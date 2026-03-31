package com.yral.shared.features.auth.data

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.di.AuthEnv
import com.yral.shared.http.HTTPEventListener
import com.yral.shared.http.exception.DNSLookupException
import com.yral.shared.testsupport.preferences.FakePreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthDataSourceImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun obtainAnonymousIdentity_retriesOnFallbackAndKeepsUsingIt() =
        runTest {
            val hosts = mutableListOf<String>()
            val resolver = SessionAuthHostResolver(CrashlyticsManager())
            val dataSource =
                createDataSource(
                    resolver = resolver,
                    onRequest = { host ->
                        hosts += host
                        when (host) {
                            PRIMARY_HOST ->
                                throw DNSLookupException(
                                    hostname = host,
                                    lookupSource = "test_dns",
                                    cause = RuntimeException("primary dns failed"),
                                )
                            FALLBACK_HOST -> tokenResponseJson("fallback-id-token")
                            else -> error("Unexpected host: $host")
                        }
                    },
                )

            val firstResponse = dataSource.obtainAnonymousIdentity()
            val secondResponse = dataSource.obtainAnonymousIdentity()

            assertEquals(listOf(PRIMARY_HOST, FALLBACK_HOST, FALLBACK_HOST), hosts)
            assertEquals("fallback-id-token", firstResponse.idToken)
            assertEquals("fallback-id-token", secondResponse.idToken)
            assertTrue(resolver.isFallbackActive())
        }

    @Test
    fun obtainAnonymousIdentity_doesNotFallbackOnNonDnsFailure() =
        runTest {
            val hosts = mutableListOf<String>()
            val resolver = SessionAuthHostResolver(CrashlyticsManager())
            val dataSource =
                createDataSource(
                    resolver = resolver,
                    onRequest = { host ->
                        hosts += host
                        throw IllegalStateException("boom")
                    },
                )

            assertFailsWith<IllegalStateException> {
                dataSource.obtainAnonymousIdentity()
            }

            assertEquals(listOf(PRIMARY_HOST), hosts)
            assertFalse(resolver.isFallbackActive())
        }

    private fun createDataSource(
        resolver: SessionAuthHostResolver = SessionAuthHostResolver(CrashlyticsManager()),
        eventListener: HTTPEventListener = NoOpHttpEventListener(),
        onRequest: suspend (host: String) -> String,
    ): AuthDataSourceImpl {
        val engine =
            MockEngine { request ->
                val body = onRequest(request.url.host)
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        return AuthDataSourceImpl(
            client = HttpClient(engine),
            json = json,
            preferences = FakePreferences(),
            authEnv = authEnv(),
            authHostResolver = resolver,
            httpEventListener = eventListener,
        )
    }

    private fun authEnv() =
        AuthEnv(
            clientId = "test-client",
            redirectUri = AuthEnv.RedirectUri(scheme = "yral"),
        )

    private fun tokenResponseJson(idToken: String): String =
        """
        {
          "id_token": "$idToken",
          "access_token": "access-token",
          "expires_in": 3600,
          "refresh_token": "refresh-token",
          "token_type": "Bearer"
        }
        """.trimIndent()

    private class NoOpHttpEventListener : HTTPEventListener {
        override fun logException(e: Exception) = Unit
    }

    private companion object {
        const val PRIMARY_HOST = "auth.dolr.ai"
        const val FALLBACK_HOST = "auth.yral.com"
    }
}
