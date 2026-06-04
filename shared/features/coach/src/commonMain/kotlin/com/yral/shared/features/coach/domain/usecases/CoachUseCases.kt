package com.yral.shared.features.coach.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.coach.domain.CoachRepository
import com.yral.shared.features.coach.domain.models.ApplyCoachProposalResult
import com.yral.shared.features.coach.domain.models.CoachMessage
import com.yral.shared.features.coach.domain.models.CoachSession
import com.yral.shared.features.coach.domain.models.SendCoachMessageResult
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

private val EXCEPTION_TYPE = ExceptionType.CHAT.name

class CreateCoachSessionUseCase(
    private val repository: CoachRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<CreateCoachSessionUseCase.Params, CoachSession>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = EXCEPTION_TYPE

    override suspend fun execute(parameter: Params): CoachSession =
        repository.createSession(botId = parameter.botId, fresh = parameter.fresh)

    data class Params(
        val botId: String,
        val fresh: Boolean = false,
    )
}

class SendCoachMessageUseCase(
    private val repository: CoachRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<SendCoachMessageUseCase.Params, SendCoachMessageResult>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = EXCEPTION_TYPE

    override suspend fun execute(parameter: Params): SendCoachMessageResult =
        repository.sendMessage(
            coachConversationId = parameter.coachConversationId,
            content = parameter.content,
            requestProposal = parameter.requestProposal,
        )

    data class Params(
        val coachConversationId: String,
        val content: String,
        val requestProposal: Boolean = false,
    )
}

class ApplyCoachProposalUseCase(
    private val repository: CoachRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<String, ApplyCoachProposalResult>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = EXCEPTION_TYPE

    override suspend fun execute(parameter: String): ApplyCoachProposalResult = repository.applyProposal(parameter)
}

class ListCoachMessagesUseCase(
    private val repository: CoachRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<String, List<CoachMessage>>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = EXCEPTION_TYPE

    override suspend fun execute(parameter: String): List<CoachMessage> = repository.listMessages(parameter)
}
