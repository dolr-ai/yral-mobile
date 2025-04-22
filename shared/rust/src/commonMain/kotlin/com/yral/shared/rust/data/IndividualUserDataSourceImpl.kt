package com.yral.shared.rust.data

import com.yral.shared.rust.base.httpPost
import com.yral.shared.rust.data.models.FeedRequestDTO
import com.yral.shared.rust.data.models.PostResponseDTO
import com.yral.shared.rust.services.IndividualUserServiceFactory
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.path
import kotlinx.serialization.json.Json

@Suppress("UnusedPrivateProperty")
class IndividualUserDataSourceImpl(
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val json: Json,
    private val client: HttpClient,
) : IndividualUserDataSource {
    override suspend fun getInitialFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO =
        httpPost(
            httpClient = client,
            json = json,
        ) {
            url {
                host = FEED_BASE_URL
                path(CACHED_FEED_PATH)
            }
            setBody(feedRequestDTO)
        }

    override suspend fun fetchMoreFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO =
        httpPost(
            httpClient = client,
            json = json,
        ) {
            url {
                host = FEED_BASE_URL
                path(ML_FEED_PATH)
            }
            setBody(feedRequestDTO)
        }

    companion object {
        private const val FEED_BASE_URL = "yral-ml-feed-server.fly.dev"
        private const val CACHED_FEED_PATH = "/api/v1/feed/coldstart/clean"
        private const val ML_FEED_PATH = "/api/v1/feed/clean"
    }
}
