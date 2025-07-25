package com.yral.android.ui.screens.account.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultAccountComponent(
    componentContext: ComponentContext,
) : AccountComponent,
    ComponentContext by componentContext,
    KoinComponent
