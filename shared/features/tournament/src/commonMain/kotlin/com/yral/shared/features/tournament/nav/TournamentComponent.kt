package com.yral.shared.features.tournament.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.features.tournament.viewmodel.TournamentViewModel

interface TournamentComponent {
    val subscriptionCoordinator: SubscriptionCoordinator?
    val lifecycle: Lifecycle

    fun processEvent(value: TournamentViewModel.Event)

    companion object {
        @Suppress("LongParameterList")
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
                isDailyTournament: Boolean,
                dailyTimeLimitMs: Long,
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

@Suppress("LongParameterList")
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
        isDailyTournament: Boolean,
        dailyTimeLimitMs: Long,
    ) -> Unit,
    private val navigateToLeaderboard: (
        tournamentId: String,
    ) -> Unit,
    private val showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
    override val subscriptionCoordinator: SubscriptionCoordinator? = null,
) : TournamentComponent,
    ComponentContext by componentContext {
    override val lifecycle: Lifecycle = componentContext.lifecycle
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
                    value.isDailyTournament,
                    value.dailyTimeLimitMs,
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
