package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.rust.service.utils.CanisterData
import org.koin.core.component.KoinComponent

internal class DefaultConversationComponent(
    componentContext: ComponentContext,
    override val requestLoginFactory: RequestLoginFactory,
    override val influencerId: String,
    override val influencerCategory: String,
    override val influencerSource: InfluencerSource,
    private val onBack: () -> Unit,
    override val openProfile: (userCanisterData: CanisterData) -> Unit,
) : ConversationComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }
}
