package com.yral.shared.features.tournament.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.data.FirebaseFunctionRequest
import com.yral.shared.features.tournament.data.models.DailySessionRequestDto
import com.yral.shared.features.tournament.data.models.DailySessionResponseDto
import com.yral.shared.features.tournament.data.models.HotOrNotVoteRequestDto
import com.yral.shared.features.tournament.data.models.HotOrNotVoteResponseDto
import com.yral.shared.features.tournament.data.models.MyTournamentsRequestDto
import com.yral.shared.features.tournament.data.models.MyTournamentsResponseDto
import com.yral.shared.features.tournament.data.models.RegisterTournamentRequestDto
import com.yral.shared.features.tournament.data.models.RegisterTournamentResponseDto
import com.yral.shared.features.tournament.data.models.TournamentLeaderboardRequestDto
import com.yral.shared.features.tournament.data.models.TournamentLeaderboardResponseDto
import com.yral.shared.features.tournament.data.models.TournamentListRequestDto
import com.yral.shared.features.tournament.data.models.TournamentListResponseDto
import com.yral.shared.features.tournament.data.models.TournamentStatusRequestDto
import com.yral.shared.features.tournament.data.models.TournamentStatusResponseDto
import com.yral.shared.features.tournament.data.models.TournamentVoteRequestDto
import com.yral.shared.features.tournament.data.models.TournamentVoteResponseDto
import com.yral.shared.features.tournament.data.models.VideoEmojisRequestDto
import com.yral.shared.features.tournament.data.models.VideoEmojisResponseDto
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

class TournamentRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : ITournamentRemoteDataSource {
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getTournaments(request: TournamentListRequestDto): TournamentListResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(TOURNAMENTS_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<TournamentListResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<TournamentListResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error getting tournaments: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getTournamentStatus(request: TournamentStatusRequestDto): TournamentStatusResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(TOURNAMENT_STATUS_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<TournamentStatusResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<TournamentStatusResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error getting tournament status: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun registerForTournament(
        idToken: String,
        request: RegisterTournamentRequestDto,
    ): RegisterTournamentResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(REGISTER_FOR_TOURNAMENT_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<RegisterTournamentResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<RegisterTournamentResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error registering for tournament: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getMyTournaments(
        idToken: String,
        request: MyTournamentsRequestDto,
    ): MyTournamentsResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(MY_TOURNAMENTS_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<MyTournamentsResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<MyTournamentsResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error getting my tournaments: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun castVote(
        idToken: String,
        request: TournamentVoteRequestDto,
    ): TournamentVoteResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(TOURNAMENT_VOTE_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<TournamentVoteResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<TournamentVoteResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error casting vote: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun castHotOrNotVote(
        idToken: String,
        request: HotOrNotVoteRequestDto,
    ): HotOrNotVoteResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(HOT_OR_NOT_VOTE_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<HotOrNotVoteResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<HotOrNotVoteResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error casting hot or not vote: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getTournamentLeaderboard(
        idToken: String,
        request: TournamentLeaderboardRequestDto,
    ): TournamentLeaderboardResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(TOURNAMENT_LEADERBOARD_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<TournamentLeaderboardResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<TournamentLeaderboardResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error getting tournament leaderboard: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getVideoEmojis(request: VideoEmojisRequestDto): VideoEmojisResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(VIDEO_EMOJIS_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(FirebaseFunctionRequest(request))
                }
            val apiResponseString = response.bodyAsText()
            return json.decodeFromString<VideoEmojisResponseDto>(apiResponseString)
        } catch (e: Exception) {
            throw YralException("Error getting video emojis: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun startDailySession(
        idToken: String,
        request: DailySessionRequestDto,
    ): DailySessionResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(START_DAILY_SESSION_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(request)
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<DailySessionResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<DailySessionResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error starting daily session: ${e.message}")
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun endDailySession(
        idToken: String,
        request: DailySessionRequestDto,
    ): DailySessionResponseDto {
        try {
            val response: HttpResponse =
                httpClient.post {
                    expectSuccess = false
                    url {
                        host = cloudFunctionUrl()
                        path(END_DAILY_SESSION_PATH)
                    }
                    val appCheckToken = firebaseAppCheckToken()
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                    }
                    setBody(request)
                }
            val apiResponseString = response.bodyAsText()
            return if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<DailySessionResponseDto.Success>(apiResponseString)
            } else {
                json.decodeFromString<DailySessionResponseDto.Error>(apiResponseString)
            }
        } catch (e: Exception) {
            throw YralException("Error ending daily session: ${e.message}")
        }
    }

    companion object {
        private const val TOURNAMENTS_PATH = "tournaments"
        private const val TOURNAMENT_STATUS_PATH = "tournament_status"
        private const val REGISTER_FOR_TOURNAMENT_PATH = "register_for_tournament"
        private const val MY_TOURNAMENTS_PATH = "my_tournaments"
        private const val TOURNAMENT_VOTE_PATH = "tournament_vote"
        private const val HOT_OR_NOT_VOTE_PATH = "hot_or_not_tournament_vote"
        private const val TOURNAMENT_LEADERBOARD_PATH = "tournament_leaderboard"
        private const val VIDEO_EMOJIS_PATH = "tournament_video_emojis"
        private const val START_DAILY_SESSION_PATH = "start_daily_session"
        private const val END_DAILY_SESSION_PATH = "end_daily_session"
        private const val HEADER_X_FIREBASE_APPCHECK = "X-Firebase-AppCheck"
    }
}
