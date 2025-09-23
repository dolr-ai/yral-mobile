package com.yral.shared.reportVideo.data

import com.yral.shared.core.AppConfigurations
import com.yral.shared.http.httpPostWithStringResponse
import com.yral.shared.reportVideo.data.models.ReportRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.path

class ReportVideoRemoteDataSource(
    private val client: HttpClient,
) : IReportVideoDataSource {
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
        private const val REPORT_VIDEO_PATH = "/api/v2/posts/report"
    }
}
