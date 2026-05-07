@file:Suppress("MagicNumber")

package com.yral.shared.features.feed.data

import com.yral.shared.features.feed.data.models.AIFeedRequestDto
import com.yral.shared.http.createClientJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedRemoteDataSourceTest {
    @Test
    fun fetchAIFeedsCallsRecommendWithMetadataEndpoint() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("recsys-influencer-feed.ansuman.yral.com", request.url.host)
                    assertEquals("/api/v1/recommend-with-metadata/user-1", request.url.encodedPath)
                    assertEquals("50", request.url.parameters["count"])
                    assertEquals("mixed", request.url.parameters["rec_type"])

                    respond(
                        content =
                            """
                            {
                              "user_id": "user-1",
                              "videos": [],
                              "count": 0,
                              "sources": {},
                              "timestamp": 123
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client =
                HttpClient(engine) {
                    defaultRequest {
                        url {
                            protocol = URLProtocol.HTTPS
                        }
                        contentType(ContentType.Application.Json)
                    }
                }
            val dataSource = FeedRemoteDataSource(client, createClientJson())

            val response =
                dataSource.fetchAIFeeds(
                    AIFeedRequestDto(
                        userId = "user-1",
                        count = 50,
                        recommendationType = "mixed",
                    ),
                )

            assertEquals("user-1", response.userId)
            assertEquals(0, response.count)
            assertEquals(123L, response.timestamp)
        }
}
