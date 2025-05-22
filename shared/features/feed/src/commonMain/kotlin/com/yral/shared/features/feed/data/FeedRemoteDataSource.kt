package com.yral.shared.features.feed.data

import com.yral.shared.core.AppConfigurations
import com.yral.shared.features.feed.data.models.ReportRequestDto
import com.yral.shared.http.httpPostWithStringResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.path

class FeedRemoteDataSource(
    private val client: HttpClient,
) : IFeedRemoteDataSource {
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
        private const val REPORT_VIDEO_PATH = "/api/v1/posts/report_v2"
    }
}
