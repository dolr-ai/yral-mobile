package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetInfluencerUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<GetInfluencerUseCase.Params, Influencer>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.UNKNOWN.name

    override suspend fun execute(parameter: Params): Influencer = chatRepository.getInfluencer(id = parameter.id)

    data class Params(
        val id: String,
    )
}
