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
    val tournamentId: String
    val initialDiamonds: Int
    val endEpochMs: Long

    fun onLeaderboardClick()

    fun onTimeUp()

    fun onBack()

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            tournamentId: String,
            initialDiamonds: Int,
            endEpochMs: Long,
            onLeaderboardClick: (tournamentId: String) -> Unit,
            onTimeUp: () -> Unit,
            onBack: () -> Unit,
        ): TournamentGameComponent =
            DefaultTournamentGameComponent(
                componentContext = componentContext,
                tournamentId = tournamentId,
                initialDiamonds = initialDiamonds,
                endEpochMs = endEpochMs,
                onLeaderboardClickCallback = onLeaderboardClick,
                onTimeUpCallback = onTimeUp,
                onBackCallback = onBack,
            )
    }
}

internal class DefaultTournamentGameComponent(
    componentContext: ComponentContext,
    override val tournamentId: String,
    override val initialDiamonds: Int,
    override val endEpochMs: Long,
    private val onLeaderboardClickCallback: (tournamentId: String) -> Unit,
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

    override fun onLeaderboardClick() {
        onLeaderboardClickCallback(tournamentId)
    }

    override fun onTimeUp() {
        onTimeUpCallback()
    }

    override fun onBack() {
        onBackCallback()
    }
}
