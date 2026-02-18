package com.yral.shared.features.aiinfluencer.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.features.auth.ui.RequestLoginFactory

interface CreateInfluencerComponent {
    fun createLoginRequestFactory(): RequestLoginFactory
    fun onBack()
    fun onProfileCreated(successMessage: String)

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            requestLoginFactory: RequestLoginFactory,
            onBack: () -> Unit,
            onProfileCreated: (successMessage: String) -> Unit,
        ): CreateInfluencerComponent =
            DefaultCreateInfluencerComponent(
                componentContext = componentContext,
                requestLoginFactory = requestLoginFactory,
                onBack = onBack,
                onProfileCreated = onProfileCreated,
            )
    }
}
