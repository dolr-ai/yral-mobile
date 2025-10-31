package com.yral.shared.features.feed.nav

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.component.KoinComponent

internal class DefaultFeedComponent(
    componentContext: ComponentContext,
    override val showAlertsOnDialog: () -> Unit,
    private val openProfile: (userCanisterData: CanisterData) -> Unit,
) : FeedComponent,
    ComponentContext by componentContext,
    KoinComponent {
    private val _openPostDetails = Channel<PostDetailsRoute?>(Channel.CONFLATED)
    override val openPostDetails: Flow<PostDetailsRoute?> = _openPostDetails.receiveAsFlow()

    override fun openPostDetails(postDetailsRoute: PostDetailsRoute) {
        Logger.d("FeedComponent") { "openPostDetails: $postDetailsRoute" }
        _openPostDetails.trySend(postDetailsRoute)
    }

    override fun openProfile(userCanisterData: CanisterData) {
        openProfile.invoke(userCanisterData)
    }
}
