package com.yral.shared.features.coach.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.coach.data.models.ApplyCoachProposalRequestDto
import com.yral.shared.features.coach.data.models.ApplyCoachProposalResponseDto
import com.yral.shared.features.coach.data.models.CoachSessionDto
import com.yral.shared.features.coach.data.models.CreateCoachSessionRequestDto
import com.yral.shared.features.coach.data.models.ListCoachMessagesResponseDto
import com.yral.shared.features.coach.data.models.SendCoachMessageRequestDto
import com.yral.shared.features.coach.data.models.SendCoachMessageResponseDto
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.path
import kotlinx.serialization.json.Json

/**
 * Coach LLM-backed POSTs (send + apply) routinely take >30s on live
 * testing. The global HttpClient timeout (30s in HttpClientFactory)
 * was cutting calls off mid-reply and showing the user "incomplete
 * messages." Bumping to 90s for these two routes only; non-LLM Coach
 * routes (createSession, listMessages) keep the global default.
 * Tracked as the mobile-side stopgap until Session 6's latency work
 * lands or token streaming ships.
 */
private const val COACH_LLM_TIMEOUT_MS = 90_000L

class CoachRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val chatBaseUrl: String,
) : CoachDataSource {
    override suspend fun createSession(
        botId: String,
        request: CreateCoachSessionRequestDto,
    ): CoachSessionDto {
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
            setBody(request)
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
            timeout { requestTimeoutMillis = COACH_LLM_TIMEOUT_MS }
        }
    }

    /**
     * PR-3 (#356, merged 2026-06-12) — body now carries the explicit
     * `proposal_id` of the card the user tapped. Pre-PR-3 the endpoint
     * implicitly picked the latest pending proposal, which silently
     * applied newer proposals when the creator scrolled up to an older
     * card. ViewModel currently passes `activeProposalMessage.id`
     * (latest unapplied) because the post-Item-1 UI has Apply only on
     * the latest proposal card; future per-card Apply would pass the
     * tapped card's id directly.
     */
    override suspend fun applyProposal(
        coachConversationId: String,
        request: ApplyCoachProposalRequestDto,
    ): ApplyCoachProposalResponseDto {
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
            setBody(request)
            timeout { requestTimeoutMillis = COACH_LLM_TIMEOUT_MS }
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
