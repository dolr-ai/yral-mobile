package com.yral.shared.app.nav

import com.arkivanov.decompose.ComponentContext

internal class DefaultSplashComponent(
    componentContext: ComponentContext,
) : SplashComponent,
    ComponentContext by componentContext
