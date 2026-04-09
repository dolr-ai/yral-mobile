package com.yral.shared.data.data

import com.yral.shared.core.AppConfigurations.DAILY_STREAK_BASE_URL
import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.data.data.models.DailyStreakDto
import com.yral.shared.data.data.models.VideoViewsDto
import com.yral.shared.http.httpDelete
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
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

    override suspend fun softDeleteInfluencer(
        principal: String,
        idToken: String,
        chatBaseUrl: String,
    ): Result<Unit> =
        runCatching {
            httpDelete(httpClient) {
                url {
                    host = chatBaseUrl
                    path(INFLUENCERS_PATH, principal)
                }
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
        }

    override suspend fun fetchDailyStreak(
        userPrincipal: String,
        idToken: String,
    ): DailyStreakDto =
        httpGet(httpClient, json) {
            url {
                host = DAILY_STREAK_BASE_URL
                path(DAILY_STREAK_PATH, userPrincipal)
            }
            bearerAuth(idToken)
        }

    companion object {
        private const val VIDEO_VIEWS_ENDPOINT = "/api/v1/rewards/videos/bulk-stats-v2"
        private const val INFLUENCERS_PATH = "api/v1/influencers"
        private const val DAILY_STREAK_PATH = "streak"
    }
}
