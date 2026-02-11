package com.yral.shared.features.tournament.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.features.tournament.viewmodel.TournamentViewModel

interface TournamentComponent {
    val subscriptionCoordinator: SubscriptionCoordinator?

    fun processEvent(value: TournamentViewModel.Event)

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            promptLogin: (pageName: SignupPageName) -> Unit,
            navigateToTournament: (
                tournamentId: String,
                title: String,
                initialDiamonds: Int,
                startEpochMs: Long,
                endEpochMs: Long,
                totalPrizePool: Int,
                isHotOrNot: Boolean,
            ) -> Unit,
            navigateToLeaderboard: (
                tournamentId: String,
            ) -> Unit,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
            subscriptionCoordinator: SubscriptionCoordinator? = null,
        ): TournamentComponent =
            DefaultTournamentComponent(
                componentContext,
                promptLogin,
                navigateToTournament,
                navigateToLeaderboard,
                showAlertsOnDialog,
                subscriptionCoordinator,
            )
    }
}

internal class DefaultTournamentComponent(
    componentContext: ComponentContext,
    private val promptLogin: (pageName: SignupPageName) -> Unit,
    private val navigateToTournament: (
        tournamentId: String,
        title: String,
        initialDiamonds: Int,
        startEpochMs: Long,
        endEpochMs: Long,
        totalPrizePool: Int,
        isHotOrNot: Boolean,
    ) -> Unit,
    private val navigateToLeaderboard: (
        tournamentId: String,
    ) -> Unit,
    private val showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
    override val subscriptionCoordinator: SubscriptionCoordinator? = null,
) : TournamentComponent,
    ComponentContext by componentContext {
    override fun processEvent(value: TournamentViewModel.Event) {
        when (value) {
            TournamentViewModel.Event.Login -> promptLogin(SignupPageName.TOURNAMENT)
            is TournamentViewModel.Event.NavigateToTournament -> {
                navigateToTournament(
                    value.tournamentId,
                    value.title,
                    value.initialDiamonds,
                    value.startEpochMs,
                    value.endEpochMs,
                    value.totalPrizePool,
                    value.isHotOrNot,
                )
            }
            is TournamentViewModel.Event.RegistrationSuccess -> {
                showAlertsOnDialog(AlertsRequestType.TOURNAMENT)
                if (value.isPro) {
                    subscriptionCoordinator?.refreshCreditBalances()
                }
            }
            is TournamentViewModel.Event.RegistrationFailed -> {
                // Handle registration failure - could show an error dialog
            }
            is TournamentViewModel.Event.NavigateToLeaderboard -> {
                navigateToLeaderboard(value.tournamentId)
            }
        }
    }
}
