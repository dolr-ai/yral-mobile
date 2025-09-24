package com.yral.shared.features.leaderboard.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.data.FirebaseFunctionRequest
import com.yral.shared.features.leaderboard.data.models.GetLeaderboardRequestDto
import com.yral.shared.features.leaderboard.data.models.LeaderboardHistoryDayDto
import com.yral.shared.features.leaderboard.data.models.LeaderboardHistoryRequestDto
import com.yral.shared.features.leaderboard.data.models.LeaderboardHistoryResponseDto
import com.yral.shared.features.leaderboard.data.models.LeaderboardResponseDto
import com.yral.shared.firebaseStore.cloudFunctionUrl
import com.yral.shared.firebaseStore.firebaseAppCheckToken
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import kotlinx.serialization.json.Json

class LeaderboardRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : ILeaderboardRemoteDataSource {
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getLeaderboard(
        idToken: String,
        request: GetLeaderboardRequestDto,
    ): LeaderboardResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(LEADERBOARD_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            val responseDto =
                if (response.status == HttpStatusCode.OK) {
                    json.decodeFromString<LeaderboardResponseDto.Success>(apiResponseString)
                } else {
                    json.decodeFromString<LeaderboardResponseDto.Error>(apiResponseString)
                }
            return responseDto
        } catch (e: Exception) {
            throw YralException("Error in getting leaderboard: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getLeaderboardHistory(
        idToken: String,
        request: LeaderboardHistoryRequestDto,
    ): LeaderboardHistoryResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(LEADERBOARD_HISTORY_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            val responseDto =
                if (response.status == HttpStatusCode.OK) {
                    val days = json.decodeFromString<List<LeaderboardHistoryDayDto>>(apiResponseString)
                    LeaderboardHistoryResponseDto.Success(days)
                } else {
                    json.decodeFromString<LeaderboardHistoryResponseDto.Error>(apiResponseString)
                }
            return responseDto
        } catch (e: Exception) {
            throw YralException("Error in getting leaderboard history: ${e.message}")
        }
    }

    companion object {
        private const val LEADERBOARD_PATH = "leaderboard_v3"
        private const val LEADERBOARD_HISTORY_PATH = "leaderboard_history"
        private const val HEADER_X_FIREBASE_APPCHECK = "X-Firebase-AppCheck"
    }
}
