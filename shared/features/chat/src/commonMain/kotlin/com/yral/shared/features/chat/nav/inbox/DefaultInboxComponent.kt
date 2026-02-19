package com.yral.shared.features.chat.nav.inbox

import com.arkivanov.decompose.ComponentContext

internal class DefaultInboxComponent(
    componentContext: ComponentContext,
) : InboxComponent(),
    ComponentContext by componentContext
