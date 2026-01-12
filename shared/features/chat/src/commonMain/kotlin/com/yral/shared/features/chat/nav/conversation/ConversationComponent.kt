package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.rust.service.utils.CanisterData

abstract class ConversationComponent {
    abstract val requestLoginFactory: RequestLoginFactory
    abstract val influencerId: String
    abstract val influencerCategory: String
    abstract val influencerSource: InfluencerSource
    abstract val openProfile: (userCanisterData: CanisterData) -> Unit
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            requestLoginFactory: RequestLoginFactory,
            influencerId: String,
            influencerCategory: String,
            influencerSource: InfluencerSource = InfluencerSource.CARD,
            onBack: () -> Unit,
            openProfile: (userCanisterData: CanisterData) -> Unit,
        ): ConversationComponent =
            DefaultConversationComponent(
                componentContext = componentContext,
                requestLoginFactory = requestLoginFactory,
                influencerId = influencerId,
                influencerCategory = influencerCategory,
                influencerSource = influencerSource,
                onBack = onBack,
                openProfile = openProfile,
            )
    }
}
