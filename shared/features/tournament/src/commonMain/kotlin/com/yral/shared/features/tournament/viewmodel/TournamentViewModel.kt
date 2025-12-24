package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.tournament.domain.GetMyTournamentsUseCase
import com.yral.shared.features.tournament.domain.GetTournamentsUseCase
import com.yral.shared.features.tournament.domain.RegisterForTournamentUseCase
import com.yral.shared.features.tournament.domain.model.GetMyTournamentsRequest
import com.yral.shared.features.tournament.domain.model.GetTournamentsRequest
import com.yral.shared.features.tournament.domain.model.RegisterForTournamentRequest
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.TournamentData
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import com.yral.shared.features.tournament.domain.model.toUiTournament
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.routing.routes.api.Tournaments
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.LinkInput
import com.yral.shared.libs.sharing.ShareService
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TournamentViewModel(
    private val sessionManager: SessionManager,
    private val getTournamentsUseCase: GetTournamentsUseCase,
    private val getMyTournamentsUseCase: GetMyTournamentsUseCase,
    private val registerForTournamentUseCase: RegisterForTournamentUseCase,
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
) : ViewModel() {
    private val tournamentDataListFlow: MutableStateFlow<List<TournamentData>> =
        MutableStateFlow(emptyList())

    private val _state: MutableStateFlow<TournamentUiState> = MutableStateFlow(TournamentUiState())
    val state: StateFlow<TournamentUiState> = _state

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = eventChannel.receiveAsFlow()
    private var loadJob: Job? = null
    private var loadRequestId = 0

    init {
        observeSessionState()
        loadTournaments()
        tournamentListUpdater()
    }

    @OptIn(ExperimentalTime::class)
    private fun tournamentListUpdater() {
        viewModelScope.launch {
            tournamentDataListFlow.collectLatest { tournamentDataList ->
                while (true) {
                    val currentTimeMs = Clock.System.now().toEpochMilliseconds()
                    val tournaments = tournamentDataList.map { it.toUiTournament() }
                    _state.update { it.copy(tournaments = tournaments) }

                    val nextBoundaryMs = findNextBoundaryTime(tournamentDataList, currentTimeMs)
                    if (nextBoundaryMs == null) {
                        break
                    }

                    val delayMs = nextBoundaryMs - currentTimeMs
                    if (delayMs > 0) {
                        delay(delayMs)
                    }
                }
            }
        }
    }

    private fun findNextBoundaryTime(
        tournamentDataList: List<TournamentData>,
        currentTimeMs: Long,
    ): Long? =
        tournamentDataList
            .flatMap { listOf(it.startEpochMs, it.endEpochMs) }
            .filter { it > currentTimeMs }
            .minOrNull()

    private fun observeSessionState() {
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    _state.update { it.copy(isLoggedIn = isSocialSignIn) }
                }
        }
    }

    fun loadTournaments() {
        loadJob?.cancel()
        val requestId = ++loadRequestId
        loadJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, error = null) }

                when (_state.value.selectedTab) {
                    TournamentUiState.Tab.All -> loadAllTournaments(requestId)
                    TournamentUiState.Tab.History -> loadMyTournaments(requestId)
                }
            }
    }

    private fun isLatestRequest(requestId: Int): Boolean = requestId == loadRequestId

    @OptIn(ExperimentalTime::class)
    private suspend fun loadAllTournaments(requestId: Int) {
        val principalId = sessionManager.userPrincipal
        getTournamentsUseCase
            .invoke(GetTournamentsRequest(principalId = principalId))
            .onSuccess { tournamentDataList ->
                if (!isLatestRequest(requestId)) {
                    return@onSuccess
                }
                tournamentDataListFlow.value = tournamentDataList
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                    )
                }
            }.onFailure { error ->
                if (!isLatestRequest(requestId)) {
                    return@onFailure
                }
                tournamentDataListFlow.value = emptyList()
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error,
                    )
                }
            }
    }

    private suspend fun loadMyTournaments(requestId: Int) {
        val principalId = sessionManager.userPrincipal
        if (principalId.isNullOrEmpty()) {
            if (isLatestRequest(requestId)) {
                _state.update {
                    it.copy(
                        tournaments = emptyList(),
                        isLoading = false,
                    )
                }
            }
            return
        }

        getMyTournamentsUseCase
            .invoke(GetMyTournamentsRequest(principalId))
            .onSuccess { tournamentDataList ->
                if (!isLatestRequest(requestId)) {
                    return@onSuccess
                }
                tournamentDataListFlow.value = tournamentDataList
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                    )
                }
            }.onFailure { error ->
                if (!isLatestRequest(requestId)) {
                    return@onFailure
                }
                _state.update {
                    tournamentDataListFlow.value = emptyList()
                    it.copy(
                        isLoading = false,
                        error = error,
                    )
                }
            }
    }

    fun onTabSelected(tab: TournamentUiState.Tab) {
        _state.update { it.copy(selectedTab = tab) }
        loadTournaments()
    }

    fun registerForTournament(tournament: Tournament) {
        val principalId = sessionManager.userPrincipal
        if (principalId.isNullOrEmpty()) {
            send(Event.Login)
            return
        }
        if (_state.value.isRegistering) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isRegistering = true) }
            val tokensRequired =
                when (val state = tournament.participationState) {
                    is TournamentParticipationState.JoinNowWithTokens -> state.tokensRequired
                    is TournamentParticipationState.RegistrationRequired -> state.tokensRequired
                    else -> 0
                }
            if (tokensRequired > 0) {
                val currentBalance =
                    sessionManager.readLatestSessionPropertyWithDefault(
                        selector = { it.coinBalance },
                        defaultValue = 0L,
                    )
                if (currentBalance < tokensRequired) {
                    _state.update { it.copy(isRegistering = false) }
                    send(
                        Event.RegistrationFailed(
                            code = TournamentErrorCodes.INSUFFICIENT_COINS,
                            message = null,
                        ),
                    )
                    return@launch
                }
            }

            registerForTournamentUseCase
                .invoke(
                    RegisterForTournamentRequest(
                        tournamentId = tournament.id,
                        principalId = principalId,
                    ),
                ).onSuccess { result ->
                    _state.update { it.copy(isRegistering = false) }
                    send(Event.RegistrationSuccess(result.tournamentId, result.coinsPaid))
                    // Refresh tournaments to update registration state
                    loadTournaments()
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isRegistering = false,
                        )
                    }
                    send(Event.RegistrationFailed(code = error.code, message = error.message))
                }
        }
    }

    fun openPrizeBreakdown(tournament: Tournament) {
        _state.update { it.copy(prizeBreakdownTournament = tournament) }
    }

    fun closePrizeBreakdown() {
        _state.update { it.copy(prizeBreakdownTournament = null) }
    }

    fun onShareClicked(tournament: Tournament) {
        viewModelScope.launch {
            val internalUrl = urlBuilder.build(Tournaments) ?: return@launch
            runCatching {
                val description = "Win up to ₹${tournament.totalPrizePool}"
                val link =
                    linkGenerator.generateShareLink(
                        LinkInput(
                            internalUrl = internalUrl,
                            title = "Join me in ${tournament.title}!",
                            description = description,
                            feature = "share_tournament",
                            tags = listOf("organic", "tournament_share"),
                            metadata = mapOf("tournament_id" to tournament.id),
                        ),
                    )
                val text = "Join me in ${tournament.title}! $description $link"
                shareService.shareText(text)
            }
        }
    }

    fun onTournamentCtaClick(tournament: Tournament) {
        if (!_state.value.isLoggedIn) {
            send(Event.Login)
            return
        }
        // Navigate to tournament details or start playing
        if (tournament.status is TournamentStatus.Ended) {
            send(
                Event.NavigateToLeaderboard(
                    tournamentId = tournament.id,
                    participantsLabel = tournament.participantsLabel,
                    scheduleLabel = tournament.scheduleLabel,
                ),
            )
        } else {
            when (tournament.participationState) {
                is TournamentParticipationState.JoinNowWithTokens ->
                    registerForTournament(
                        tournament,
                    )

                is TournamentParticipationState.RegistrationRequired ->
                    registerForTournament(
                        tournament,
                    )

                TournamentParticipationState.JoinNow ->
                    send(
                        Event.NavigateToTournament(
                            tournamentId = tournament.id,
                            initialDiamonds = tournament.initialDiamonds,
                            endEpochMs = tournament.endEpochMs,
                        ),
                    )
                else -> {}
            }
        }
    }

    fun onNoHistoryCtaClicked() {
        if (state.value.isLoggedIn) {
            onTabSelected(TournamentUiState.Tab.All)
        } else {
            send(Event.Login)
        }
    }

    fun onStartPlayingClicked() {
        _state.update { it.copy(showHowToPlayTournament = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun send(event: Event) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    sealed class Event {
        data object Login : Event()
        data class RegistrationSuccess(
            val tournamentId: String,
            val coinsPaid: Int,
        ) : Event()

        data class RegistrationFailed(
            val code: TournamentErrorCodes,
            val message: String?,
        ) : Event()

        data class NavigateToTournament(
            val tournamentId: String,
            val initialDiamonds: Int,
            val endEpochMs: Long,
        ) : Event()

        data class NavigateToLeaderboard(
            val tournamentId: String,
            val participantsLabel: String,
            val scheduleLabel: String,
        ) : Event()
    }
}
