package com.yral.shared.features.feed.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow

interface FeedComponent {
    val requestLoginFactory: RequestLoginFactory
    val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    val openPostDetails: Flow<PostDetailsRoute?>
    val promptLogin: (pendingRoute: AppRoute) -> Unit
    val openLeaderboard: () -> Unit
    val openWallet: () -> Unit
    fun openPostDetails(postDetailsRoute: PostDetailsRoute)
    fun openProfile(userCanisterData: CanisterData)

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            requestLoginFactory: RequestLoginFactory,
            openProfile: (userCanisterData: CanisterData) -> Unit,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
            promptLogin: (pendingRoute: AppRoute) -> Unit,
            openLeaderboard: () -> Unit,
            openWallet: () -> Unit,
        ): FeedComponent =
            DefaultFeedComponent(
                componentContext,
                requestLoginFactory,
                showAlertsOnDialog,
                openProfile,
                promptLogin,
                openLeaderboard,
                openWallet,
            )
    }
}
