package com.yral.shared.features.tournament.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.tournament.viewmodel.TournamentViewModel

interface TournamentComponent {
    fun processEvent(value: TournamentViewModel.Event)

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            promptLogin: (pageName: SignupPageName) -> Unit,
        ): TournamentComponent =
            DefaultTournamentComponent(
                componentContext,
                promptLogin,
            )
    }
}

internal class DefaultTournamentComponent(
    componentContext: ComponentContext,
    private val promptLogin: (pageName: SignupPageName) -> Unit,
) : TournamentComponent,
    ComponentContext by componentContext {
    override fun processEvent(value: TournamentViewModel.Event) {
        when (value) {
            TournamentViewModel.Event.Login -> promptLogin(SignupPageName.TOURNAMENT)
        }
    }
}
