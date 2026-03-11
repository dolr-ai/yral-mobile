package com.yral.shared.features.chat.data

import co.touchlab.kermit.Logger
import com.yral.shared.core.AppConfigurations
import com.yral.shared.features.chat.data.models.ChatAccessApiResponse
import com.yral.shared.features.chat.data.models.GrantChatAccessRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.path
import kotlinx.serialization.json.Json

interface ChatAccessBillingDataSource {
    val packageName: String
    suspend fun grantChatAccess(request: GrantChatAccessRequestDto): ChatAccessApiResponse
    suspend fun checkChatAccess(
        userId: String,
        botId: String,
    ): ChatAccessApiResponse
}

class ChatAccessBillingRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    override val packageName: String,
) : ChatAccessBillingDataSource {
    companion object {
        private const val TAG = "ChatAccessBilling"
        private const val GRANT_ENDPOINT = "google/chat-access/grant"
        private const val CHECK_ENDPOINT = "google/chat-access/check"
    }

    override suspend fun grantChatAccess(request: GrantChatAccessRequestDto): ChatAccessApiResponse {
        Logger.d(TAG) { "grantChatAccess request: botId=${request.botId}, productId=${request.productId}" }
        val response =
            httpClient.post {
                expectSuccess = false
                url {
                    host = AppConfigurations.BILLING_BASE_URL
                    path(GRANT_ENDPOINT)
                }
                setBody(request)
            }
        val body = response.bodyAsText()
        Logger.d(TAG) { "grantChatAccess status=${response.status}, response: $body" }
        return json.decodeFromString<ChatAccessApiResponse>(body)
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
                    host = AppConfigurations.BILLING_BASE_URL
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
