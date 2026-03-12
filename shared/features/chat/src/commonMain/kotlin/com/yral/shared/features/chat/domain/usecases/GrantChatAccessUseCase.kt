package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.data.ChatAccessBillingDataSource
import com.yral.shared.features.chat.data.models.GrantChatAccessRequestDto
import com.yral.shared.features.chat.domain.models.ChatAccessStatus
import com.yral.shared.features.chat.domain.models.GrantError
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class GrantChatAccessParams(
    val botId: String,
    val purchaseToken: String,
    val productId: String,
)

class GrantChatAccessUseCase(
    private val dataSource: ChatAccessBillingDataSource,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<GrantChatAccessParams, ChatAccessStatus>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.CHAT.name

    @OptIn(ExperimentalTime::class)
    override suspend fun execute(parameter: GrantChatAccessParams): ChatAccessStatus {
        val request =
            GrantChatAccessRequestDto(
                packageName = dataSource.packageName,
                productId = parameter.productId,
                purchaseToken = parameter.purchaseToken,
                botId = parameter.botId,
            )
        val result = dataSource.grantChatAccess(request)
        val httpStatus = result.httpStatus
        val response = result.apiResponse

        when {
            httpStatus in HTTP_OK_RANGE && response.success -> {
                val data = response.data
                return ChatAccessStatus(
                    hasAccess = data?.hasAccess == true,
                    expiresAtMs = data?.expiresAt?.let { parseIso8601ToEpochMs(it) },
                )
            }
            httpStatus in HTTP_CLIENT_ERROR_RANGE ->
                throw GrantError.ClientError(response.error ?: response.msg ?: "Unknown client error")
            else ->
                throw GrantError.ServerError(httpStatus)
        }
    }

    companion object {
        private val HTTP_OK_RANGE = 200..299
        private val HTTP_CLIENT_ERROR_RANGE = 400..499
    }

    @OptIn(ExperimentalTime::class)
    private fun parseIso8601ToEpochMs(timestamp: String): Long? =
        runCatching {
            val normalized =
                if (timestamp.endsWith('Z') || timestamp.matches(Regex(".*[+-]\\d{2}:\\d{2}$"))) {
                    timestamp
                } else {
                    "${timestamp}Z"
                }
            Instant.parse(normalized).toEpochMilliseconds()
        }.getOrNull()
}
