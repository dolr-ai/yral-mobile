package com.yral.shared.features.chat.nav.inbox

import com.arkivanov.decompose.ComponentContext

@Suppress("UtilityClassWithPublicConstructor")
abstract class InboxComponent {
    companion object Companion {
        operator fun invoke(componentContext: ComponentContext): InboxComponent =
            DefaultInboxComponent(
                componentContext = componentContext,
            )
    }
}
