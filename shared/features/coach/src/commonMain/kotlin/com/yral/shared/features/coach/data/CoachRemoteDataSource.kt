package com.yral.shared.features.coach.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.coach.data.models.ApplyCoachProposalResponseDto
import com.yral.shared.features.coach.data.models.CoachSessionDto
import com.yral.shared.features.coach.data.models.ListCoachMessagesResponseDto
import com.yral.shared.features.coach.data.models.SendCoachMessageRequestDto
import com.yral.shared.features.coach.data.models.SendCoachMessageResponseDto
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.path
import kotlinx.serialization.json.Json

class CoachRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val chatBaseUrl: String,
) : CoachDataSource {
    override suspend fun createSession(botId: String): CoachSessionDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(COACH_CONVERSATIONS_PATH, botId)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun sendMessage(
        coachConversationId: String,
        request: SendCoachMessageRequestDto,
    ): SendCoachMessageResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(COACH_CONVERSATIONS_PATH, coachConversationId, MESSAGES_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            setBody(request)
        }
    }

    override suspend fun applyProposal(coachConversationId: String): ApplyCoachProposalResponseDto {
        val idToken = getIdToken()
        return httpPost(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(COACH_CONVERSATIONS_PATH, coachConversationId, APPLY_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    override suspend fun listMessages(coachConversationId: String): ListCoachMessagesResponseDto {
        val idToken = getIdToken()
        return httpGet(
            httpClient = httpClient,
            json = json,
        ) {
            url {
                host = chatBaseUrl
                path(COACH_CONVERSATIONS_PATH, coachConversationId, MESSAGES_PATH)
            }
            headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
        }
    }

    private suspend fun getIdToken() =
        preferences.getString(PrefKeys.ID_TOKEN.name)
            ?: throw YralException("Authorisation not found")

    private companion object {
        const val COACH_CONVERSATIONS_PATH = "api/v1/creator/coach/conversations"
        const val MESSAGES_PATH = "messages"
        const val APPLY_PATH = "apply"
    }
}
