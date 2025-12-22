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
            navigateToTournament: (tournamentId: String) -> Unit = {},
        ): TournamentComponent =
            DefaultTournamentComponent(
                componentContext,
                promptLogin,
                navigateToTournament,
            )
    }
}

internal class DefaultTournamentComponent(
    componentContext: ComponentContext,
    private val promptLogin: (pageName: SignupPageName) -> Unit,
    private val navigateToTournament: (tournamentId: String) -> Unit,
) : TournamentComponent,
    ComponentContext by componentContext {
    override fun processEvent(value: TournamentViewModel.Event) {
        when (value) {
            TournamentViewModel.Event.Login -> promptLogin(SignupPageName.TOURNAMENT)
            is TournamentViewModel.Event.NavigateToTournament -> {
                navigateToTournament(value.tournamentId)
            }
            is TournamentViewModel.Event.RegistrationSuccess -> {
                // Handle registration success - could show a toast or navigate
            }
            is TournamentViewModel.Event.RegistrationFailed -> {
                // Handle registration failure - could show an error dialog
            }
        }
    }
}
