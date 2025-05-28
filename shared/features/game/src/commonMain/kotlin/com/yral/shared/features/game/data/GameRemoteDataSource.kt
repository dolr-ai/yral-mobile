package com.yral.shared.features.game.data

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.AppConfigurations.FIREBASE_AUTH_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.game.data.models.CastVoteRequestDto
import com.yral.shared.features.game.data.models.CastVoteResponseDto
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase
import io.ktor.client.HttpClient
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
    private val getIdTokenUseCase: GetIdTokenUseCase,
    private val getConfigUseCase: GetFBDocumentUseCase<GameConfigDto>,
    private val getAboutUseCase: GetCollectionUseCase<AboutGameItemDto>,
) : IGameRemoteDataSource {
    override suspend fun getConfig(): GameConfigDto =
        getConfigUseCase
            .invoke(
                parameter =
                    GetFBDocumentUseCase.Params(
                        collectionPath = GAME_CONFIG_COLLECTION,
                        documentId = GAME_CONFIG_DOCUMENT,
                    ),
            ).getOrThrow()

    override suspend fun getRules(): List<AboutGameItemDto> =
        getAboutUseCase
            .invoke(GAME_ABOUT_COLLECTION)
            .getOrThrow()

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun castVote(request: CastVoteRequestDto): CastVoteResponseDto {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        try {
            val response: HttpResponse =
                httpClient.post {
                    url {
                        host = FIREBASE_AUTH_URL
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

    companion object {
        private const val GAME_CONFIG_COLLECTION = "config"
        private const val GAME_CONFIG_DOCUMENT = "smiley_game_v1"
        private const val GAME_ABOUT_COLLECTION = "smiley_game_rules"
        private const val CAST_VOTE_PATH = "cast_vote"
    }
}
