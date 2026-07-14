package com.yral.shared.features.chat.data

import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.data.models.ChatAccessApiResponse
import com.yral.shared.features.chat.data.models.GrantChatAccessRequestDto
import com.yral.shared.features.chat.data.models.GrantResult
import com.yral.shared.features.chat.data.models.toPlatformGrantRequestBody
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.path
import kotlinx.serialization.json.Json

internal expect fun getGrantChatAccessEndpoint(): String

internal expect fun getBotSubscriptionGrantEndpoint(): String

interface ChatAccessBillingDataSource {
    val packageName: String
    suspend fun grantChatAccess(request: GrantChatAccessRequestDto): GrantResult

    /**
     * Verifies/records a per-bot auto-renewing subscription purchase
     * ("bot_sub_*" products). Same request shape as [grantChatAccess];
     * the backend acknowledges the Google subscription server-side.
     */
    suspend fun grantBotSubscription(request: GrantChatAccessRequestDto): GrantResult

    suspend fun checkChatAccess(
        userId: String,
        botId: String,
    ): ChatAccessApiResponse
}

class ChatAccessBillingRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val billingBaseUrl: String,
    override val packageName: String,
) : ChatAccessBillingDataSource {
    companion object {
        private const val TAG = "ChatAccessBilling"
        private const val CHECK_ENDPOINT = "google/chat-access/check"
    }

    override suspend fun grantChatAccess(request: GrantChatAccessRequestDto): GrantResult =
        postGrant(
            endpoint = getGrantChatAccessEndpoint(),
            request = request,
            logLabel = "grantChatAccess",
        )

    override suspend fun grantBotSubscription(request: GrantChatAccessRequestDto): GrantResult =
        postGrant(
            endpoint = getBotSubscriptionGrantEndpoint(),
            request = request,
            logLabel = "grantBotSubscription",
        )

    private suspend fun postGrant(
        endpoint: String,
        request: GrantChatAccessRequestDto,
        logLabel: String,
    ): GrantResult {
        Logger.d(TAG) { "$logLabel request: botId=${request.botId}, productId=${request.productId}" }
        val response =
            httpClient.post {
                expectSuccess = false
                url {
                    host = billingBaseUrl
                    path(endpoint)
                }
                setBody(request.toPlatformGrantRequestBody())
            }
        val body = response.bodyAsText()
        Logger.d(TAG) { "$logLabel status=${response.status}, response: $body" }
        return GrantResult(
            httpStatus = response.status.value,
            apiResponse = json.decodeFromString<ChatAccessApiResponse>(body),
        )
    }

    override suspend fun checkChatAccess(
        userId: String,
        botId: String,
    ): ChatAccessApiResponse {
        Logger.d(TAG) { "checkChatAccess request: userId=$userId, botId=$botId" }
        val response =
            httpClient.get {
                expectSuccess = false
                url {
                    host = billingBaseUrl
                    path(CHECK_ENDPOINT)
                }
                parameter("user_id", userId)
                parameter("bot_id", botId)
            }
        val body = response.bodyAsText()
        Logger.d(TAG) { "checkChatAccess status=${response.status}, response: $body" }
        return json.decodeFromString<ChatAccessApiResponse>(body)
    }
}
