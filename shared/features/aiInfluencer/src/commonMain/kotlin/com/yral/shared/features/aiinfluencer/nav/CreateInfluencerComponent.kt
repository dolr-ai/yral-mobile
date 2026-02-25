package com.yral.shared.features.aiinfluencer.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.features.auth.ui.RequestLoginFactory

interface CreateInfluencerComponent {
    var source: BotCreationSource
    fun createLoginRequestFactory(): RequestLoginFactory
    fun onBack()
    fun onProfileCreated(successMessage: String)

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            source: BotCreationSource,
            requestLoginFactory: RequestLoginFactory,
            onBack: () -> Unit,
            onProfileCreated: (successMessage: String) -> Unit,
        ): CreateInfluencerComponent =
            DefaultCreateInfluencerComponent(
                componentContext = componentContext,
                source = source,
                requestLoginFactory = requestLoginFactory,
                onBack = onBack,
                onProfileCreated = onProfileCreated,
            )
    }
}
