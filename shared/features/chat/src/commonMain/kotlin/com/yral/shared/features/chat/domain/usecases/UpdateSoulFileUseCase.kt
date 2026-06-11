package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.SoulFile
import com.yral.shared.features.chat.domain.models.SoulFileSection
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class UpdateSoulFileUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<UpdateSoulFileUseCase.Params, SoulFile>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): SoulFile =
        chatRepository.updateSoulFile(
            botId = parameter.botId,
            sections = parameter.sections,
            expectedSectionsVersionSha256 = parameter.expectedSectionsVersionSha256,
        )

    /**
     * Coach pivot Bucket 2 — pass [expectedSectionsVersionSha256] = the
     * sha received on the last GET /soul-file. Backend returns 409 if
     * the sections changed concurrently; ViewModel handles 409 by
     * re-GETting and surfacing the "Soul File changed on another device,
     * reload?" dialog (never auto-merging plain text — lossy).
     */
    data class Params(
        val botId: String,
        val sections: List<SoulFileSection>,
        val expectedSectionsVersionSha256: String,
    )
}
