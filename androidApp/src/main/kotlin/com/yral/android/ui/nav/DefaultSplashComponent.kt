package com.yral.android.ui.nav

import com.arkivanov.decompose.ComponentContext

internal class DefaultSplashComponent(
    componentContext: ComponentContext,
) : SplashComponent,
    ComponentContext by componentContext
