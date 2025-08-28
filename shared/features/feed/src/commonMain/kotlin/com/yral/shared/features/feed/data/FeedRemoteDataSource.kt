package com.yral.shared.features.feed.data

import com.yral.shared.core.AppConfigurations
import com.yral.shared.core.AppConfigurations.FEED_BASE_URL
import com.yral.shared.features.feed.data.models.FeedRequestDTO
import com.yral.shared.features.feed.data.models.PostResponseDTO
import com.yral.shared.features.feed.data.models.ReportRequestDto
import com.yral.shared.http.httpPost
import com.yral.shared.http.httpPostWithStringResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.path
import kotlinx.serialization.json.Json

class FeedRemoteDataSource(
    private val client: HttpClient,
    private val json: Json,
) : IFeedRemoteDataSource {
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

    override suspend fun reportVideo(request: ReportRequestDto): String =
        httpPostWithStringResponse(
            httpClient = client,
        ) {
            url {
                host = AppConfigurations.OFF_CHAIN_BASE_URL
                path(REPORT_VIDEO_PATH)
            }
            setBody(request)
        }

    companion object {
        private const val CACHED_FEED_PATH = "/recommendations/cache"
        private const val ML_FEED_PATH = "/recommendations"
        private const val REPORT_VIDEO_PATH = "/api/v2/posts/report"
    }
}
