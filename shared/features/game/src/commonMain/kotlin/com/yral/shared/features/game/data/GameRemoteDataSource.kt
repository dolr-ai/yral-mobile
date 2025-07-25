package com.yral.shared.features.game.data

import com.yral.shared.core.AppConfigurations.PUMP_DUMP_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.game.data.models.CastVoteRequestDto
import com.yral.shared.features.game.data.models.CastVoteResponseDto
import com.yral.shared.features.game.data.models.GetBalanceResponseDto
import com.yral.shared.firebaseStore.cloudFunctionUrl
import com.yral.shared.http.httpGet
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import kotlinx.serialization.json.Json

class GameRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : IGameRemoteDataSource {
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun castVote(
        idToken: String,
        request: CastVoteRequestDto,
    ): CastVoteResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(CAST_VOTE_PATH)
                    }
                    headers.append("authorization", "Bearer $idToken")
                    setBody(request)
                }
            val apiResponseString = response.bodyAsText()
            val responseDto =
                if (response.status == HttpStatusCode.OK) {
                    json.decodeFromString<CastVoteResponseDto.Success>(apiResponseString)
                } else {
                    json.decodeFromString<CastVoteResponseDto.Error>(apiResponseString)
                }
            return responseDto
        } catch (e: Exception) {
            throw YralException("Error in casting vote: ${e.message}")
        }
    }

    override suspend fun getBalance(userPrincipal: String): GetBalanceResponseDto =
        httpGet(
            httpClient,
            json,
        ) {
            url {
                host = PUMP_DUMP_BASE_URL
                path(GET_BALANCE_PATH, userPrincipal)
            }
        }

    companion object {
        private const val CAST_VOTE_PATH = "cast_vote"
        private const val GET_BALANCE_PATH = "v2/balance"
    }
}
