package com.yral.android.ui.screens.feed.nav

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import org.koin.core.component.KoinComponent

internal class DefaultFeedComponent(
    componentContext: ComponentContext,
) : FeedComponent,
    ComponentContext by componentContext,
    KoinComponent {
    private val _openPostDetails = Channel<PostDetailsRoute?>(Channel.CONFLATED)
    override val openPostDetails: Flow<PostDetailsRoute?> = _openPostDetails.consumeAsFlow()

    override fun openPostDetails(postDetailsRoute: PostDetailsRoute) {
        Logger.d("FeedComponent") { "openPostDetails: $postDetailsRoute" }
        _openPostDetails.trySend(postDetailsRoute)
    }
}
