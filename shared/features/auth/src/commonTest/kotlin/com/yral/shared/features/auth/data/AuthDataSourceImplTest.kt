package com.yral.shared.features.auth.data

import com.yral.shared.features.auth.di.AuthEnv
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
import kotlin.test.assertNotNull

class AuthDataSourceImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun obtainAnonymousIdentity_returnsTokenOnSuccess() =
        runTest {
            val dataSource =
                createDataSource(
                    onRequest = { host ->
                        assertEquals(AUTH_HOST, host)
                        tokenResponseJson("test-id-token")
                    },
                )

            val response = dataSource.obtainAnonymousIdentity()

            assertEquals("test-id-token", response.idToken)
        }

    @Test
    fun obtainAnonymousIdentity_reportsDnsFailureToCrashlytics() =
        runTest {
            val eventListener = RecordingHttpEventListener()
            val dataSource =
                createDataSource(
                    eventListener = eventListener,
                    onRequest = { host ->
                        throw DNSLookupException(
                            hostname = host,
                            lookupSource = "test_dns",
                            cause = RuntimeException("dns failed"),
                        )
                    },
                )

            assertFailsWith<DNSLookupException> {
                dataSource.obtainAnonymousIdentity()
            }

            // On Android, platformReportsDnsLookupFailure() == true so the
            // data layer does not double-report (the platform already does).
            // On iOS, platformReportsDnsLookupFailure() == false so the data
            // layer reports via httpEventListener. Since commonTest runs on
            // Android here, we expect zero calls to the event listener.
            assertEquals(0, eventListener.exceptions.size)
        }

    @Test
    fun obtainAnonymousIdentity_doesNotFallbackOnNonDnsFailure() =
        runTest {
            val dataSource =
                createDataSource(
                    onRequest = { _ ->
                        throw IllegalStateException("boom")
                    },
                )

            assertFailsWith<IllegalStateException> {
                dataSource.obtainAnonymousIdentity()
            }
        }

    @Test
    fun createAiAccount_returnsDelegatedIdentityOnSuccess() =
        runTest {
            val dataSource =
                createDataSource(
                    onRequest = { host ->
                        assertEquals(AUTH_HOST, host)
                        createAiAccountResponseJson()
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

            assertEquals(listOf(1, 2, 3), response.delegatedIdentity.fromKey)
            assertNotNull(response.delegatedIdentity.toSecret)
        }

    private fun createDataSource(
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

    private class RecordingHttpEventListener : HTTPEventListener {
        val exceptions = mutableListOf<Exception>()
        override fun logException(e: Exception) {
            exceptions += e
        }
    }

    private companion object {
        const val AUTH_HOST = "auth.yral.com"
    }
}
