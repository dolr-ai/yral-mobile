package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.yral.shared.core.session.ProDetails
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.game.domain.GetBalanceUseCase
import com.yral.shared.features.tournament.analytics.TournamentTelemetry
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
import com.yral.shared.features.tournament.domain.model.TournamentType
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

@Suppress("TooManyFunctions")
class TournamentViewModel(
    private val sessionManager: SessionManager,
    private val getTournamentsUseCase: GetTournamentsUseCase,
    private val getMyTournamentsUseCase: GetMyTournamentsUseCase,
    private val registerForTournamentUseCase: RegisterForTournamentUseCase,
    private val getBalanceUseCase: GetBalanceUseCase,
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
    private val telemetry: TournamentTelemetry,
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

    fun trackScreenViewed(tournament: Tournament) {
        telemetry.onTournamentScreenViewed(
            tournamentId = tournament.id,
            tournamentType = tournament.type,
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun tournamentListUpdater() {
        viewModelScope.launch {
            tournamentDataListFlow.collectLatest { tournamentDataList ->
                var previousTournaments: List<Tournament> = emptyList()
                while (true) {
                    val currentTimeMs = Clock.System.now().toEpochMilliseconds()
                    val tournaments = tournamentDataList.map { it.toUiTournament(_state.value.proDetails) }

                    trackTournamentStateChanges(previousTournaments, tournaments)
                    previousTournaments = tournaments

                    _state.update { it.copy(tournaments = tournaments) }

                    val nextBoundaryMs =
                        findNextBoundaryTime(tournamentDataList, currentTimeMs) ?: break

                    val delayMs = nextBoundaryMs - currentTimeMs
                    if (delayMs > 0) {
                        // Add small buffer (100ms) to ensure time comparison happens after boundary
                        delay(delayMs + BOUNDARY_BUFFER_MS)
                    }
                }
            }
        }
    }

    private fun trackTournamentStateChanges(
        previousTournaments: List<Tournament>,
        currentTournaments: List<Tournament>,
    ) {
        val previousStateMap = previousTournaments.associateBy { it.id }
        currentTournaments.forEach { tournament ->
            val previousTournament = previousStateMap[tournament.id] ?: return@forEach
            telemetry.onTournamentStateChangedIfChanged(
                previousTournament = previousTournament,
                currentTournament = tournament,
            )
        }
    }

    private fun findNextBoundaryTime(
        tournamentDataList: List<TournamentData>,
        currentTimeMs: Long,
    ): Long? {
        // Include all boundary times:
        // - startEpochMs: when tournament goes live
        // - endEpochMs: when tournament ends
        // - startEpochMs - 10 minutes: when JoinNowDisabled activates for registered users
        return tournamentDataList
            .flatMap {
                listOf(
                    it.startEpochMs - TEN_MINUTES_MS, // 10 min before start (JoinNowDisabled boundary)
                    it.startEpochMs, // tournament goes live
                    it.endEpochMs, // tournament ends
                )
            }.filter { it > currentTimeMs }
            .minOrNull()
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    _state.update { it.copy(isLoggedIn = isSocialSignIn) }

                    if (isSocialSignIn) {
                        processPendingTournamentRegistration()
                    }
                }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.proDetails },
                    defaultValue = ProDetails(),
                ).collect { proDetails ->
                    _state.update { state ->
                        state.copy(
                            proDetails = proDetails,
                            tournaments =
                                tournamentDataListFlow
                                    .value
                                    .map { it.toUiTournament(proDetails) },
                        )
                    }
                }
        }
    }

    private fun processPendingTournamentRegistration() {
        viewModelScope.launch {
            // Wait for tournaments to load, balance to be fetched, and Firebase login
            repeat(MAX_PENDING_TOURNAMENT_RETRIES) {
                val pendingTournamentId =
                    sessionManager.consumePendingTournamentRegistrationId() ?: return@launch

                val pendingTournament = _state.value.tournaments.find { it.id == pendingTournamentId }
                val balanceLoaded = sessionManager.isCoinBalanceLoaded()
                val firebaseLoggedIn = sessionManager.isFirebaseLoggedIn()

                if (pendingTournament != null && balanceLoaded && firebaseLoggedIn) {
                    onTournamentCtaClick(pendingTournament)
                    return@launch
                }
                // Not ready yet, put the ID back and wait
                sessionManager.setPendingTournamentRegistrationId(pendingTournamentId)
                delay(PENDING_TOURNAMENT_RETRY_DELAY_MS)
            }
            // After retries, clear pending to avoid stuck state
            sessionManager.consumePendingTournamentRegistrationId()
        }
    }

    companion object {
        private const val MAX_PENDING_TOURNAMENT_RETRIES = 10
        private const val PENDING_TOURNAMENT_RETRY_DELAY_MS = 500L
        private const val BOUNDARY_BUFFER_MS = 100L
        private const val TEN_MINUTES_MS = 10 * 60 * 1000L
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
                        tournaments = tournamentDataList.map { data -> data.toUiTournament(it.proDetails) },
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
                        tournaments = tournamentDataList.map { data -> data.toUiTournament(it.proDetails) },
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

    @Suppress("LongMethod", "MagicNumber")
    fun registerForTournament(tournament: Tournament) {
        val principalId = sessionManager.userPrincipal ?: return
        if (_state.value.isRegistering) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isRegistering = true) }
            val tokensRequired =
                when (val state = tournament.participationState) {
                    is TournamentParticipationState.JoinNowWithCredit -> 0
                    is TournamentParticipationState.JoinNowWithTokens -> state.tokensRequired
                    is TournamentParticipationState.RegistrationRequired -> state.tokensRequired
                    else -> 0
                }

            // Track registration initiated
            val currentBalance =
                sessionManager.readLatestSessionPropertyWithDefault(
                    selector = { it.coinBalance },
                    defaultValue = 0L,
                )
            val durationMinutes = ((tournament.endEpochMs - tournament.startEpochMs) / 60000).toInt()
            telemetry.onRegistrationInitiated(
                tournamentId = tournament.id,
                tournamentType = tournament.type,
                entryFeePoints = tokensRequired,
                userPointBalance = currentBalance.toInt(),
                tournamentDurationMinutes = durationMinutes,
            )

            if (tokensRequired > 0 && currentBalance < tokensRequired) {
                _state.update { it.copy(isRegistering = false) }
                send(
                    Event.RegistrationFailed(
                        code = TournamentErrorCodes.INSUFFICIENT_COINS,
                        message = null,
                    ),
                )
                return@launch
            }

            val isPro = tournament.participationState is TournamentParticipationState.JoinNowWithCredit

            registerForTournamentUseCase
                .invoke(
                    RegisterForTournamentRequest(
                        tournamentId = tournament.id,
                        principalId = principalId,
                        isPro = isPro,
                    ),
                ).onSuccess { result ->
                    _state.update { it.copy(isRegistering = false) }
                    // Track registration success
                    telemetry.onTournamentRegistered(
                        tournamentId = result.tournamentId,
                        tournamentType = tournament.type,
                        entryFeePoints = result.coinsPaid,
                    )
                    // Refresh balance from server after entry fee was deducted
                    refreshBalance(principalId)
                    send(
                        Event.RegistrationSuccess(
                            result.tournamentId,
                            result.coinsPaid,
                            isPro,
                        ),
                    )
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
            // Store tournament ID in SessionManager (survives ViewModel recreation)
            sessionManager.setPendingTournamentRegistrationId(tournament.id)
            send(Event.Login)
            return
        }
        // Navigate to tournament details or start playing
        if (tournament.status is TournamentStatus.Ended) {
            send(
                Event.NavigateToLeaderboard(
                    tournamentId = tournament.id,
                ),
            )
        } else {
            when (tournament.participationState) {
                is TournamentParticipationState.JoinNowWithCredit,
                is TournamentParticipationState.JoinNowWithTokens,
                ->
                    registerForTournament(
                        tournament,
                    )

                is TournamentParticipationState.RegistrationRequired ->
                    registerForTournament(
                        tournament,
                    )

                is TournamentParticipationState.JoinNow ->
                    send(
                        Event.NavigateToTournament(
                            tournamentId = tournament.id,
                            title = tournament.title,
                            initialDiamonds = tournament.participationState.userDiamonds,
                            startEpochMs = tournament.startEpochMs,
                            endEpochMs = tournament.endEpochMs,
                            totalPrizePool = tournament.totalPrizePool,
                            isHotOrNot = tournament.type == TournamentType.HOT_OR_NOT,
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

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun refreshBalance(principalId: String) {
        viewModelScope.launch {
            getBalanceUseCase
                .invoke(principalId)
                .onSuccess { newBalance ->
                    sessionManager.updateCoinBalance(newBalance)
                }
        }
    }

    private fun send(event: Event) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    sealed class Event {
        data object Login : Event()
        data class RegistrationSuccess(
            val tournamentId: String,
            val coinsPaid: Int,
            val isPro: Boolean,
        ) : Event()

        data class RegistrationFailed(
            val code: TournamentErrorCodes,
            val message: String?,
        ) : Event()

        data class NavigateToTournament(
            val tournamentId: String,
            val title: String,
            val initialDiamonds: Int,
            val startEpochMs: Long,
            val endEpochMs: Long,
            val totalPrizePool: Int,
            val isHotOrNot: Boolean,
        ) : Event()

        data class NavigateToLeaderboard(
            val tournamentId: String,
        ) : Event()
    }
}
