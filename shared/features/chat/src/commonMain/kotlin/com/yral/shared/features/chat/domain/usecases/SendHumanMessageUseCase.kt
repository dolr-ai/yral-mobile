package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

/**
 * H2H send. Mirrors [SendMessageUseCase] but routes through the
 * `POST /api/v1/chat/human/conversations/{id}/messages` endpoint.
 *
 * The result shape ([SendMessageResult] with `assistantMessage = null`
 * for H2H since the other side responds asynchronously via WebSocket
 * inbox push, not in this response) is identical to the AI path so the
 * call site that swaps the optimistic Local for the server-truth
 * Remote stays the same.
 */
class SendHumanMessageUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<SendHumanMessageUseCase.Params, SendMessageResult>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): SendMessageResult =
        chatRepository.sendHumanMessage(
            conversationId = parameter.conversationId,
            draft = parameter.draft,
        )

    data class Params(
        val conversationId: String,
        val draft: SendMessageDraft,
    )
}
