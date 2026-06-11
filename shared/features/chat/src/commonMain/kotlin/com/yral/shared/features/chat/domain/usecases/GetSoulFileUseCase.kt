package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.SoulFile
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetSoulFileUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<GetSoulFileUseCase.Params, SoulFile>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): SoulFile =
        chatRepository.getSoulFile(botId = parameter.botId)

    data class Params(
        val botId: String,
    )
}
