package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import com.yral.shared.analytics.constants.Features
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DuplicatePostsEvent(
    override val event: String = FeatureEvents.DUPLICATE_POSTS.getEventName(),
    override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("duplicate_posts") val duplicatePosts: Int,
    @SerialName("total_fetched") val totalPosts: Int,
) : EventData
