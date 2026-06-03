package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

/**
 * Creates (or fetches the existing) 1:1 H2H conversation between the
 * authenticated user and the other-user identified by [Params.participantPrincipalId].
 *
 * The repository / backend are idempotent — calling twice in quick
 * succession with the same participant returns the same conversation,
 * so the profile-screen "Send Message" tap handler doesn't need its
 * own de-dup guard.
 */
class CreateHumanConversationUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<CreateHumanConversationUseCase.Params, Conversation>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): Conversation =
        chatRepository.createHumanConversation(participantId = parameter.participantPrincipalId)

    data class Params(
        val participantPrincipalId: String,
    )
}
