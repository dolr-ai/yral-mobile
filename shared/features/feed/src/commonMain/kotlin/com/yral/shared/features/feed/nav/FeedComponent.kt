package com.yral.shared.features.feed.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow

interface FeedComponent {
    val showAlertsOnDialog: () -> Unit
    val openPostDetails: Flow<PostDetailsRoute?>
    fun openPostDetails(postDetailsRoute: PostDetailsRoute)
    fun openProfile(userCanisterData: CanisterData)

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            openProfile: (userCanisterData: CanisterData) -> Unit,
            showAlertsOnDialog: () -> Unit,
        ): FeedComponent = DefaultFeedComponent(componentContext, showAlertsOnDialog, openProfile)
    }
}
