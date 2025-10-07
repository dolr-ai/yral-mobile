package com.yral.shared.features.feed.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import kotlinx.coroutines.flow.Flow

interface FeedComponent {
    val openPostDetails: Flow<PostDetailsRoute?>
    fun openPostDetails(postDetailsRoute: PostDetailsRoute)

    companion object Companion {
        operator fun invoke(componentContext: ComponentContext): FeedComponent = DefaultFeedComponent(componentContext)
    }
}
