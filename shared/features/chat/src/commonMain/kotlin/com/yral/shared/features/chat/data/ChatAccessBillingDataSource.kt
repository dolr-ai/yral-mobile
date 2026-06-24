package com.yral.shared.features.chat.data

import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.data.models.ChatAccessApiResponse
import com.yral.shared.features.chat.data.models.GrantAppleChatAccessRequestDto
import com.yral.shared.features.chat.data.models.GrantChatAccessRequestDto
import com.yral.shared.features.chat.data.models.GrantResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json

internal expect fun isAppleChatBillingPlatform(): Boolean

interface ChatAccessBillingDataSource {
    val packageName: String
    suspend fun grantChatAccess(request: GrantChatAccessRequestDto): GrantResult
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
        private const val GOOGLE_GRANT_ENDPOINT = "google/chat-access/grant"
        private const val APPLE_GRANT_ENDPOINT = "apple/chat-access/grant"
        private const val CHECK_ENDPOINT = "google/chat-access/check"
    }

    override suspend fun grantChatAccess(request: GrantChatAccessRequestDto): GrantResult {
        Logger.d(TAG) { "grantChatAccess request: botId=${request.botId}, productId=${request.productId}" }
        val isApple = isAppleChatBillingPlatform()
        val endpoint = if (isApple) APPLE_GRANT_ENDPOINT else GOOGLE_GRANT_ENDPOINT
        val requestBody =
            if (isApple) {
                GrantAppleChatAccessRequestDto(
                    transactionId = request.purchaseToken,
                    productId = request.productId,
                    botId = request.botId,
                )
            } else {
                request
            }
        val response =
            httpClient.post {
                expectSuccess = false
                url {
                    applyBillingBaseUrl(billingBaseUrl)
                    path(endpoint)
                }
                setBody(requestBody)
            }
        val body = response.bodyAsText()
        Logger.d(TAG) { "grantChatAccess status=${response.status}, response: $body" }
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
                    applyBillingBaseUrl(billingBaseUrl)
                    path(CHECK_ENDPOINT)
                }
                parameter("user_id", userId)
                parameter("bot_id", botId)
            }
        val body = response.bodyAsText()
        Logger.d(TAG) { "checkChatAccess status=${response.status}, response: $body" }
        return json.decodeFromString<ChatAccessApiResponse>(body)
    }

    private fun URLBuilder.applyBillingBaseUrl(baseUrl: String) {
        val normalized = if ("://" in baseUrl) baseUrl else "https://$baseUrl"
        takeFrom(normalized)
    }
}
