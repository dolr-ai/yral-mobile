package com.yral.shared.features.aiinfluencer.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.features.auth.ui.RequestLoginFactory

internal class DefaultCreateInfluencerComponent(
    componentContext: ComponentContext,
    override var source: BotCreationSource,
    private val requestLoginFactory: RequestLoginFactory,
    private val onBack: () -> Unit,
    private val onProfileCreated: (successMessage: String) -> Unit,
) : CreateInfluencerComponent,
    ComponentContext by componentContext {
    override fun createLoginRequestFactory(): RequestLoginFactory = requestLoginFactory

    override fun onBack() {
        onBack.invoke()
    }

    override fun onProfileCreated(successMessage: String) {
        onProfileCreated.invoke(successMessage)
    }
}
