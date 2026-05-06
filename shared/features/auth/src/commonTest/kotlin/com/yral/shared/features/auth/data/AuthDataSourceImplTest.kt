package com.yral.shared.features.auth.data

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.di.AuthEnv
import com.yral.shared.features.auth.di.notificationEnvironmentForAppId
import com.yral.shared.http.HTTPEventListener
import com.yral.shared.http.exception.DNSLookupException
import com.yral.shared.testsupport.preferences.FakePreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
                            PRIMARY_HOST -> {
                                throw DNSLookupException(
                                    hostname = host,
                                    lookupSource = "test_dns",
                                    cause = RuntimeException("primary dns failed"),
                                )
                            }

                            FALLBACK_HOST -> {
                                tokenResponseJson("fallback-id-token")
                            }

                            else -> {
                                error("Unexpected host: $host")
                            }
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

    @Test
    fun createAiAccount_retriesOnFallbackAndUsesResolvedHost() =
        runTest {
            val hosts = mutableListOf<String>()
            val resolver = SessionAuthHostResolver(CrashlyticsManager())
            val dataSource =
                createDataSource(
                    resolver = resolver,
                    onRequest = { host ->
                        hosts += host
                        when (host) {
                            PRIMARY_HOST -> {
                                throw DNSLookupException(
                                    hostname = host,
                                    lookupSource = "test_dns",
                                    cause = RuntimeException("primary dns failed"),
                                )
                            }

                            FALLBACK_HOST -> {
                                createAiAccountResponseJson()
                            }

                            else -> {
                                error("Unexpected host: $host")
                            }
                        }
                    },
                )

            val response =
                dataSource.createAiAccount(
                    userPrincipal = "fg3u2-hyjkt-itbo5-fgmut-vzibq-z623z-npczw-drwra-frlmf-dr2bz-2qe",
                    signature = byteArrayOf(1, 2, 3),
                    publicKey = byteArrayOf(4, 5, 6),
                    signedMessage = byteArrayOf(),
                    ingressExpirySecs = 1775740307,
                    ingressExpiryNanos = 111476128,
                    delegations = null,
                )

            assertEquals(listOf(PRIMARY_HOST, FALLBACK_HOST), hosts)
            assertTrue(resolver.isFallbackActive())
            assertEquals(listOf(1, 2, 3), response.delegatedIdentity.fromKey)
            assertNotNull(response.delegatedIdentity.toSecret)
        }

    @Test
    fun notificationEnvironmentForAppId_mapsKnownApps() {
        assertEquals("staging", notificationEnvironmentForAppId("com.yral.android"))
        assertEquals("staging", notificationEnvironmentForAppId("com.yral.iosApp.staging"))
        assertEquals("production", notificationEnvironmentForAppId("com.yral.android.app"))
        assertEquals("production", notificationEnvironmentForAppId("com.yral.iosApp"))
    }

    @Test
    fun notificationEnvironmentForAppId_rejectsUnknownApps() {
        assertFailsWith<IllegalStateException> {
            notificationEnvironmentForAppId("com.yral.unknown")
        }
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
            client =
                HttpClient(engine) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                    defaultRequest {
                        contentType(ContentType.Application.Json)
                    }
                },
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
            notificationEnvironment = "staging",
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

    private fun createAiAccountResponseJson(): String =
        """
        {
          "delegated_identity": {
            "from_key": [1, 2, 3],
            "to_secret": {
              "kty": "EC",
              "crv": "secp256k1",
              "x": "x-value",
              "y": "y-value",
              "d": "d-value"
            },
            "delegation_chain": [
              {
                "signature": [7, 8, 9],
                "delegation": {
                  "pubkey": [10, 11, 12],
                  "expiration": 1776344742052174366,
                  "targets": null
                }
              }
            ]
          }
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
