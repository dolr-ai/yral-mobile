package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.feed.data.models.FeedDetailsCache
import com.yral.shared.features.feed.data.models.toFeedDetails
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.Preferences
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class LoadCachedFeedDetailsUseCase(
    private val preferences: Preferences,
    private val json: Json,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<LoadCachedFeedDetailsUseCase.Params, List<FeedDetails>?>(
        appDispatchers.disk,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.FEED.name

    override suspend fun execute(parameter: Params): List<FeedDetails>? {
        val cacheKey = getCacheKey(parameter.userPrincipal)
        val cachedJson = preferences.getString(cacheKey) ?: return null

        val cachedData = json.decodeFromString<FeedDetailsCache>(cachedJson)
        val currentTime = Clock.System.now()
        val cacheAge = currentTime - cachedData.timestamp
        val expirationDuration = getCacheExpirationDuration()

        // Remove cache after checking (whether valid or expired)
        preferences.remove(cacheKey)

        return if (cacheAge <= expirationDuration) {
            // Cache is valid, return feed details
            cachedData.feedDetails.map { it.toFeedDetails() }
        } else {
            // Cache expired
            null
        }
    }

    companion object {
        private const val CACHE_EXPIRATION_DAYS = 3
        private fun getCacheKey(userPrincipal: String) = "feed_cache_$userPrincipal"

        @Suppress("MagicNumber")
        private fun getCacheExpirationDuration(): Duration = Duration.parse("${CACHE_EXPIRATION_DAYS * 24}h")
    }

    data class Params(
        val userPrincipal: String,
    )
}
