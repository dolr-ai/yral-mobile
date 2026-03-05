package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

@Suppress("LongParameterList")
internal class DefaultProfileMainComponent(
    componentContext: ComponentContext,
    override val requestLoginFactory: RequestLoginFactory,
    override val subscriptionCoordinator: SubscriptionCoordinator,
    override val pendingVideoNavigation: Flow<String?>,
    override val userCanisterData: CanisterData?,
    override val showBackButton: Boolean,
    private val onUploadVideoClicked: () -> Unit,
    private val openAccountSheet: () -> Unit,
    private val openAccount: () -> Unit,
    private val openEditProfile: () -> Unit,
    private val openProfile: (CanisterData) -> Unit,
    private val openCreateInfluencer: (source: BotCreationSource) -> Unit,
    private val openConversation: (OpenConversationParams) -> Unit,
    private val onBackClicked: () -> Unit,
    override val showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
) : ProfileMainComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun onUploadVideoClick() {
        onUploadVideoClicked.invoke()
    }

    override fun openAccountSheet() {
        openAccountSheet.invoke()
    }

    override fun openAccount() {
        openAccount.invoke()
    }

    override fun openEditProfile() {
        openEditProfile.invoke()
    }

    override fun openProfile(userCanisterData: CanisterData) {
        openProfile.invoke(userCanisterData)
    }

    override fun openCreateInfluencer() {
        openCreateInfluencer.invoke(BotCreationSource.PROFILE_PAGE)
    }

    override fun openConversation(params: OpenConversationParams) {
        openConversation.invoke(params)
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }
}
