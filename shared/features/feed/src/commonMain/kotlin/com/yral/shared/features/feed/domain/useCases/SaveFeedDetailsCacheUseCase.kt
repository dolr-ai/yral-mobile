package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.feed.data.models.FeedDetailsCache
import com.yral.shared.features.feed.data.models.toFeedDetailsForCache
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.Preferences
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SaveFeedDetailsCacheUseCase(
    private val preferences: Preferences,
    private val json: Json,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<SaveFeedDetailsCacheUseCase.Params, Unit>(
        appDispatchers.disk,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.FEED.name

    override suspend fun execute(parameter: Params) {
        if (parameter.feedDetails.isEmpty()) return

        val cacheData =
            FeedDetailsCache(
                timestamp = Clock.System.now(),
                feedDetails = parameter.feedDetails.map { it.toFeedDetailsForCache() },
            )
        val cacheKey = getCacheKey(parameter.userPrincipal)
        val cacheJsonString = json.encodeToString(cacheData)
        preferences.putString(cacheKey, cacheJsonString)
    }

    companion object {
        private fun getCacheKey(userPrincipal: String) = "feed_cache_$userPrincipal"
    }

    data class Params(
        val userPrincipal: String,
        val feedDetails: List<FeedDetails>,
    )
}
