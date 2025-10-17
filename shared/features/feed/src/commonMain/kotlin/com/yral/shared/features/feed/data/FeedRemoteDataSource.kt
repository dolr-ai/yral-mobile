package com.yral.shared.features.feed.data

import com.yral.shared.core.AppConfigurations.AI_FEED_BASE_URL
import com.yral.shared.core.AppConfigurations.FEED_BASE_URL
import com.yral.shared.features.feed.data.models.AIFeedRequestDto
import com.yral.shared.features.feed.data.models.AIPostResponseDTO
import com.yral.shared.features.feed.data.models.FeedRequestDTO
import com.yral.shared.features.feed.data.models.PostResponseDTO
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.path
import kotlinx.serialization.json.Json

class FeedRemoteDataSource(
    private val client: HttpClient,
    private val json: Json,
) : IFeedDataSource {
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

    override suspend fun fetchAIFeeds(feedRequest: AIFeedRequestDto): AIPostResponseDTO =
        httpGet(
            httpClient = client,
            json = json,
        ) {
            url {
                host = AI_FEED_BASE_URL
                path(AI_ML_FEED_PATH, feedRequest.userId)
                parameters.append("count", feedRequest.count.toString())
                parameters.append("rec_type", feedRequest.recommendationType)
            }
        }

    companion object {
        private const val CACHED_FEED_PATH = "v2/recommendations/cache"
        private const val ML_FEED_PATH = "v2/recommendations"
        private const val AI_ML_FEED_PATH = "recommend-with-metadata"
    }
}
