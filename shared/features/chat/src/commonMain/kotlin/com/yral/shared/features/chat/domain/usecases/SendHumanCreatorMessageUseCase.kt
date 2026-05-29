package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class SendHumanCreatorMessageUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<SendHumanCreatorMessageUseCase.Params, ChatMessage>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): ChatMessage =
        chatRepository.sendHumanCreatorMessage(
            conversationId = parameter.conversationId,
            content = parameter.content,
        )

    data class Params(
        val conversationId: String,
        val content: String,
    )
}
