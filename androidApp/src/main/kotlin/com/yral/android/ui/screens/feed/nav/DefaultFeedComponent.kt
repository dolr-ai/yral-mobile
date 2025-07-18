package com.yral.android.ui.screens.feed.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultFeedComponent(
    componentContext: ComponentContext,
) : FeedComponent,
    ComponentContext by componentContext,
    KoinComponent
