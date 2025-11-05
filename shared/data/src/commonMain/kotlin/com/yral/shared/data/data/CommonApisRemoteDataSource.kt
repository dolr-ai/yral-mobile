package com.yral.shared.data.data

import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.data.data.models.VideoViewsDto
import com.yral.shared.http.httpPost
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.path
import kotlinx.serialization.json.Json

class CommonApisRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : CommonApisDataSource {
    override suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViewsDto> =
        httpPost(httpClient, json) {
            url {
                host = OFF_CHAIN_BASE_URL
                path(VIDEO_VIEWS_ENDPOINT)
            }
            setBody(mapOf("video_ids" to videoId))
        }

    companion object {
        private const val VIDEO_VIEWS_ENDPOINT = "/api/v1/rewards/videos/bulk-stats-v2"
    }
}
