package com.yral.shared.features.chat.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.DiscoverySearchResult
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class SearchDiscoveryUseCase(
    private val chatRepository: ChatRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<SearchDiscoveryUseCase.Params, List<DiscoverySearchResult>>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): List<DiscoverySearchResult> =
        chatRepository.searchDiscovery(query = parameter.query, limit = parameter.limit)

    data class Params(
        val query: String,
        val limit: Int = DEFAULT_LIMIT,
    ) {
        companion object {
            const val DEFAULT_LIMIT = 20
        }
    }
}
