package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class SendMessageUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<SendMessageUseCase.Params, SendMessageResult>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): SendMessageResult =
        chatRepository.sendMessage(
            conversationId = parameter.conversationId,
            draft = parameter.draft,
        )

    data class Params(
        val conversationId: String,
        val draft: SendMessageDraft,
    )
}
