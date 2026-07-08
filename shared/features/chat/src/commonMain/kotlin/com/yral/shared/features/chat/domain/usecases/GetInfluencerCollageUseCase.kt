package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.Collage
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetInfluencerCollageUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<GetInfluencerCollageUseCase.Params, Collage>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): Collage =
        chatRepository.getInfluencerCollage(
            influencerId = parameter.influencerId,
            isSubscribed = parameter.isSubscribed,
        )

    data class Params(
        val influencerId: String,
        val isSubscribed: Boolean,
    )
}
