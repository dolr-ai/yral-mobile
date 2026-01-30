package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow

interface ProfileMainComponent {
    val requestLoginFactory: RequestLoginFactory
    val subscriptionCoordinator: SubscriptionCoordinator
    val pendingVideoNavigation: Flow<String?>
    val userCanisterData: CanisterData?
    val showBackButton: Boolean
    val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    fun onUploadVideoClick()
    fun openAccount()
    fun openEditProfile()
    fun openProfile(userCanisterData: CanisterData)
    fun openConversation(
        influencerId: String,
        influencerCategory: String,
        influencerSource: InfluencerSource = InfluencerSource.CARD,
    )
    fun onBackClicked()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            requestLoginFactory: RequestLoginFactory,
            subscriptionCoordinator: SubscriptionCoordinator,
            userCanisterData: CanisterData? = null,
            pendingVideoNavigation: Flow<String?>,
            onUploadVideoClicked: () -> Unit,
            openAccount: () -> Unit,
            openEditProfile: () -> Unit,
            openProfile: (CanisterData) -> Unit,
            openConversation: (
                influencerId: String,
                influencerCategory: String,
                influencerSource: InfluencerSource,
            ) -> Unit,
            onBackClicked: () -> Unit,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
            showBackButton: Boolean = false,
        ): ProfileMainComponent =
            DefaultProfileMainComponent(
                componentContext = componentContext,
                requestLoginFactory = requestLoginFactory,
                subscriptionCoordinator = subscriptionCoordinator,
                userCanisterData = userCanisterData,
                pendingVideoNavigation = pendingVideoNavigation,
                onUploadVideoClicked = onUploadVideoClicked,
                openAccount = openAccount,
                openEditProfile = openEditProfile,
                openProfile = openProfile,
                openConversation = openConversation,
                onBackClicked = onBackClicked,
                showAlertsOnDialog = showAlertsOnDialog,
                showBackButton = showBackButton,
            )
    }
}
