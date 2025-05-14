package com.yral.shared.features.feed.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.libs.useCase.SuspendUseCase
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post

class FetchFeedDetailsUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
) : SuspendUseCase<Post, FeedDetails>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Post): FeedDetails =
        individualUserRepository
            .fetchFeedDetails(parameter)
}
