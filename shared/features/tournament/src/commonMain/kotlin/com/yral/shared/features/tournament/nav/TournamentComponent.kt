package com.yral.shared.features.tournament.nav

import com.arkivanov.decompose.ComponentContext

interface TournamentComponent {
    companion object {
        operator fun invoke(componentContext: ComponentContext): TournamentComponent =
            DefaultTournamentComponent(
                componentContext,
            )
    }
}

internal class DefaultTournamentComponent(
    componentContext: ComponentContext,
) : TournamentComponent,
    ComponentContext by componentContext
