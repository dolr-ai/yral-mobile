package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.data.ChatAccessBillingDataSource
import com.yral.shared.features.chat.domain.models.ChatAccessStatus
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class CheckChatAccessUseCase(
    private val dataSource: ChatAccessBillingDataSource,
    private val sessionManager: SessionManager,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<String, ChatAccessStatus>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.CHAT.name

    @OptIn(ExperimentalTime::class)
    override suspend fun execute(parameter: String): ChatAccessStatus {
        val userId =
            sessionManager.userPrincipal
                ?: throw IllegalStateException("User not signed in")
        val response = dataSource.checkChatAccess(userId = userId, botId = parameter)
        val data = response.data
        return ChatAccessStatus(
            hasAccess = data?.hasAccess == true,
            expiresAtMs = data?.expiresAt?.let { parseIso8601ToEpochMs(it) },
        )
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
