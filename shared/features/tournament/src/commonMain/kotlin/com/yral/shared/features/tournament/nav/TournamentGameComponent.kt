package com.yral.shared.features.tournament.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.component.KoinComponent

interface TournamentGameComponent : FeedComponent {
    val gameConfig: TournamentGameConfig

    fun onLeaderboardClick(showResult: Boolean = false)

    fun onTimeUp()

    fun onBack()

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            tournamentId: String,
            tournamentTitle: String,
            initialDiamonds: Int,
            startEpochMs: Long,
            endEpochMs: Long,
            totalPrizePool: Int,
            onLeaderboardClick: (tournamentId: String, showResult: Boolean) -> Unit,
            onTimeUp: () -> Unit,
            onBack: () -> Unit,
        ): TournamentGameComponent =
            DefaultTournamentGameComponent(
                componentContext = componentContext,
                gameConfig =
                    TournamentGameConfig(
                        tournamentId = tournamentId,
                        tournamentTitle = tournamentTitle,
                        initialDiamonds = initialDiamonds,
                        totalPrizePool = totalPrizePool,
                        startEpochMs = startEpochMs,
                        endEpochMs = endEpochMs,
                    ),
                onLeaderboardClickCallback = onLeaderboardClick,
                onTimeUpCallback = onTimeUp,
                onBackCallback = onBack,
            )
    }

    data class TournamentGameConfig(
        val tournamentId: String,
        val tournamentTitle: String = "",
        val initialDiamonds: Int,
        val totalPrizePool: Int,
        val startEpochMs: Long,
        val endEpochMs: Long,
    )
}

internal class DefaultTournamentGameComponent(
    componentContext: ComponentContext,
    override val gameConfig: TournamentGameComponent.TournamentGameConfig,
    private val onLeaderboardClickCallback: (tournamentId: String, showResult: Boolean) -> Unit,
    private val onTimeUpCallback: () -> Unit,
    private val onBackCallback: () -> Unit,
) : TournamentGameComponent,
    ComponentContext by componentContext,
    KoinComponent {
    private val _openPostDetails = Channel<PostDetailsRoute?>(Channel.CONFLATED)
    override val openPostDetails: Flow<PostDetailsRoute?> = _openPostDetails.receiveAsFlow()

    override val showAlertsOnDialog: (type: AlertsRequestType) -> Unit = {}
    override val promptLogin: (pendingRoute: AppRoute) -> Unit = {}
    override val openLeaderboard: () -> Unit = { onLeaderboardClick() }
    override val openWallet: () -> Unit = {}

    override fun openPostDetails(postDetailsRoute: PostDetailsRoute) {
        _openPostDetails.trySend(postDetailsRoute)
    }

    override fun openProfile(userCanisterData: CanisterData) {
        // Not implemented for tournament game
    }

    override fun onLeaderboardClick(showResult: Boolean) {
        onLeaderboardClickCallback(gameConfig.tournamentId, showResult)
    }

    override fun onTimeUp() {
        onTimeUpCallback()
    }

    override fun onBack() {
        onBackCallback()
    }
}
