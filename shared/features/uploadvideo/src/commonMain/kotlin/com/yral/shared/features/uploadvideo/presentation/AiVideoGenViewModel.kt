package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.analytics.events.AiVideoGenFailureType
import com.yral.shared.analytics.events.CreditFeature
import com.yral.shared.analytics.events.SubscriptionEntryPoint
import com.yral.shared.analytics.events.VideoCreationType
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.session.ProDetails
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.subscriptions.analytics.SubscriptionTelemetry
import com.yral.shared.features.uploadvideo.analytics.UploadVideoTelemetry
import com.yral.shared.features.uploadvideo.data.remote.models.TokenType
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetFreeCreditsStatusUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.PollAndUploadAiVideoUseCase
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.domain.models.RateLimitStatus
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.ai_video_subscription_nudge_description
import yral_mobile.shared.features.uploadvideo.generated.resources.ai_video_subscription_nudge_title
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
@Suppress("TooManyFunctions")
class AiVideoGenViewModel internal constructor(
    private val requiredUseCases: RequiredUseCases,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
    private val uploadVideoTelemetry: UploadVideoTelemetry,
    private val subscriptionTelemetry: SubscriptionTelemetry,
    logger: YralLogger,
    flagManager: FeatureFlagManager,
) : ViewModel() {
    private val logger = logger.withTag(AiVideoGenViewModel::class.simpleName ?: "")

    private val _state =
        MutableStateFlow(
            ViewState(
                isSubscriptionEnabled = flagManager.isEnabled(AppFeatureFlags.Common.EnableSubscription),
            ),
        )
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private val aiVideoGenEventChannel = Channel<AiVideoGenEvent>(Channel.CONFLATED)
    val aiVideoGenEvents = aiVideoGenEventChannel.receiveAsFlow()

    val sessionObserver =
        sessionManager.observeSessionStateWithProperty { state, properties ->
            val canisterId =
                when (state) {
                    is SessionState.SignedIn -> state.session.canisterId
                    else -> null
                }
            canisterId to properties.coinBalance
        }

    private var currentRequestKey: VideoGenRequestKey? = null
    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    _state.update { it.copy(isLoggedIn = isSocialSignIn) }
                }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.proDetails },
                    defaultValue = ProDetails(),
                ).collect { proDetails ->
                    _state.update { it.copy(proDetails = proDetails) }
                    if (_state.value.currentCanister != null) {
                        getFreeCreditsStatus()
                    }
                }
        }
    }

    fun refresh(canisterId: String) {
        val currentCanister = _state.value.currentCanister
        val isCanisterChanged = currentCanister != null && currentCanister != canisterId
        when {
            currentCanister == null -> {
                logger.d { "Null: Setting current canister to $canisterId" }
                _state.update { it.copy(currentCanister = canisterId) }
            }

            isCanisterChanged -> {
                logger.d { "Mismatch: Setting current canister to $canisterId" }
                _state.update { it.copy(currentCanister = canisterId) }
            }

            else -> logger.d { "Same canister" }
        }
        when (_state.value.uiState) {
            is UiState.Initial -> {
                _state.value.currentCanister?.let {
                    loadProviders()
                    getFreeCreditsStatus()
                }
            }

            is UiState.InProgress -> {
                if (isCanisterChanged) {
                    logger.d { "Canister changed, cancelling polling" }
                    cleanup()
                    loadProviders()
                    getFreeCreditsStatus()
                } else {
                    logger.d { "Canister unchanged, reusing polling" }
                }
            }

            else -> Unit
        }
    }

    private fun loadProviders() {
        viewModelScope.launch {
            val currentSelected = _state.value.selectedProvider
            _state.update { it.copy(providers = emptyList(), selectedProvider = null) }
            requiredUseCases
                .getProviders()
                .onSuccess { list ->
                    if (list.isNotEmpty()) {
                        _state.update {
                            it.copy(
                                providers = list,
                                selectedProvider =
                                    currentSelected?.let { selected ->
                                        list.find { provider -> provider.id == selected.id }
                                    } ?: list.first(),
                            )
                        }
                    }
                }.onFailure { logger.e(it) { "Error fetching providers" } }
        }
    }

    private fun getFreeCreditsStatus() {
        viewModelScope.launch {
            with(_state.value.proDetails) {
                if (isProPurchased) {
                    _state.update {
                        it.copy(
                            usedCredits = totalCredits - availableCredits,
                            totalCredits = totalCredits,
                        )
                    }
                    uploadVideoTelemetry.videoCreationPageViewed(
                        type = VideoCreationType.AI_VIDEO,
                        creditsFetched = true,
                        creditsAvailable = availableCredits,
                    )
                    return@launch
                }
            }
            _state.update { it.copy(usedCredits = null) }
            sessionManager.userPrincipal?.let { userPrincipal ->
                requiredUseCases
                    .getFreeCreditsStatus(
                        parameter =
                            GetFreeCreditsStatusUseCase.Params(
                                userPrincipal = userPrincipal,
                                isRegistered =
                                    sessionManager.readLatestSessionPropertyWithDefault(
                                        selector = { it.isSocialSignIn },
                                        defaultValue = false,
                                    ),
                            ),
                    ).onSuccess { status ->
                        val usedCredits = status.usedCredits()
                        uploadVideoTelemetry.videoCreationPageViewed(
                            type = VideoCreationType.AI_VIDEO,
                            creditsFetched = true,
                            creditsAvailable = 1 - usedCredits,
                        )
                        _state
                            .update { it.copy(usedCredits = usedCredits) }
                            .also { logger.d { "Used credits ${_state.value.usedCredits} $status" } }
                        setSubscriptionNudgeShown(usedCredits)
                    }.onFailure { error ->
                        uploadVideoTelemetry.videoCreationPageViewed(
                            type = VideoCreationType.AI_VIDEO,
                            creditsFetched = false,
                        )
                        logger.e(error) { "Error fetching free credits" }
                        _state.update { it.copy(usedCredits = null) }
                    }
            }
        }
    }

    private fun RateLimitStatus.usedCredits() =
        if (_state.value.isLoggedIn) {
            if (isLimited) 1 else 0
        } else {
            0
        }

    @Suppress("LongMethod")
    fun generateAiVideo() {
        viewModelScope.launch {
            val currentState = _state.value
            currentState.selectedProvider?.let { selectedProvider ->
                sessionManager.userPrincipal?.let { userId ->
                    _state.update { it.copy(uiState = UiState.InProgress(0f)) }
                    currentRequestKey = null
                    requiredUseCases
                        .generateVideo(
                            parameter =
                                GenerateVideoUseCase.Param(
                                    params =
                                        GenerateVideoParams(
                                            providerId = selectedProvider.id,
                                            prompt = currentState.prompt.trim(),
                                            aspectRatio = selectedProvider.defaultAspectRatio,
                                            durationSeconds = selectedProvider.defaultDuration,
                                            generateAudio = if (selectedProvider.supportsAudio == true) true else null,
                                            tokenType =
                                                if (currentState.isCreditsAvailable()) {
                                                    if (currentState.proDetails.isProPurchased) {
                                                        TokenType.YRAL_PRO_SUBSCRIPTION
                                                    } else {
                                                        TokenType.FREE
                                                    }
                                                } else {
                                                    TokenType.SATS
                                                },
                                            userId = userId,
                                        ),
                                ),
                        ).onSuccess { result ->
                            logger.d { "Video generated: $result" }
                            result.requestKey?.let { requestKey ->
                                currentRequestKey = requestKey
                                reserveBalance()
                                pollAndUploadVideo(
                                    modelName = selectedProvider.name,
                                    prompt = currentState.prompt.trim(),
                                    requestKey = requestKey,
                                )
                                return@onSuccess
                            }
                            result.providerError?.let { error ->
                                pushTriggerFailed(
                                    model = selectedProvider.name,
                                    prompt = currentState.prompt.trim(),
                                    reason = error,
                                )
                                _state.update {
                                    it.copy(bottomSheetType = BottomSheetType.Error(error, true))
                                }
                            }
                        }.onFailure { error ->
                            logger.e(error) { "Error generating video" }
                            pushTriggerFailed(
                                model = selectedProvider.name,
                                prompt = currentState.prompt.trim(),
                                reason = error.message ?: "",
                            )
                            _state.update {
                                it.copy(bottomSheetType = BottomSheetType.Error("", true))
                            }
                        }
                }
            }
        }
    }

    private fun reserveBalance() {
        if (_state.value.isCreditsAvailable()) return
        _state.value.selectedProvider?.let { selectedProvider ->
            val reservedBalance = selectedProvider.cost?.sats
            reservedBalance?.let { cost ->
                _state.update { it.copy(reservedBalance = cost) }
                _state.value.currentBalance?.let { balance ->
                    sessionManager.updateCoinBalance(balance.minus(cost))
                }
            }
        }
    }

    private fun returnBalance() {
        with(_state.value) {
            reservedBalance?.let { reserved ->
                _state.update { it.copy(reservedBalance = null) }
                _state.value.currentBalance?.let { balance ->
                    sessionManager.updateCoinBalance(balance.plus(reserved))
                }
            }
        }
    }

    private fun pushTriggerFailed(
        model: String,
        prompt: String,
        reason: String,
    ) {
        uploadVideoTelemetry.aiVideoGenerated(
            model = model,
            prompt = prompt,
            isSuccess = false,
            reason = reason,
            reasonType = AiVideoGenFailureType.TRIGGER_FAILED,
        )
    }

    @Suppress("LongMethod")
    private fun pollAndUploadVideo(
        modelName: String,
        prompt: String,
        requestKey: VideoGenRequestKey,
    ) {
        pollingJob?.cancel()
        pollingJob =
            viewModelScope.launch {
                val userPrincipal = sessionManager.userPrincipal ?: return@launch
                requiredUseCases
                    .pollAndUploadAiVideo
                    .invoke(
                        parameters =
                            PollAndUploadAiVideoUseCase.Params(
                                userPrincipal = userPrincipal,
                                modelName = modelName,
                                prompt = prompt,
                                requestKey = requestKey,
                                isFastInitially = false,
                                hashtags = emptyList(),
                                description = "",
                                isNsfw = false,
                                enableHotOrNot = false,
                            ),
                    ).collect { result ->
                        result.fold(
                            success = { pollResult ->
                                when (pollResult) {
                                    is PollAndUploadAiVideoUseCase.PollAndUploadResult.InProgress -> {
                                        _state.update { it.copy(uiState = UiState.InProgress(0f)) }
                                    }

                                    is PollAndUploadAiVideoUseCase.PollAndUploadResult.Success -> {
                                        logger.d { "Generated video uploaded successfully" }
                                        _state.update {
                                            it.copy(
                                                uiState = UiState.Success(pollResult.videoUrl),
                                                reservedBalance = null,
                                            )
                                        }
                                        if (_state.value.proDetails.isProPurchased) {
                                            // Track credit consumption
                                            val creditsRemaining =
                                                _state.value.proDetails.availableCredits - 1
                                            subscriptionTelemetry.onCreditsConsumed(
                                                feature = CreditFeature.AI_VIDEO,
                                                creditsUsed = 1,
                                                creditsRemaining = creditsRemaining.coerceAtLeast(0),
                                            )
                                            aiVideoGenEventChannel.trySend(AiVideoGenEvent.RefreshProDetails)
                                        }
                                    }

                                    is PollAndUploadAiVideoUseCase.PollAndUploadResult.Failed -> {
                                        _state.update {
                                            it.copy(
                                                bottomSheetType =
                                                    BottomSheetType.Error(
                                                        pollResult.errorMessage,
                                                        true,
                                                    ),
                                            )
                                        }
                                        // if endFlow true then only return balance
                                        returnBalance()
                                        if (_state.value.proDetails.isProPurchased) {
                                            aiVideoGenEventChannel.trySend(AiVideoGenEvent.RefreshProDetails)
                                        }
                                    }

                                    is PollAndUploadAiVideoUseCase.PollAndUploadResult.UploadFailed -> {
                                        _state.update {
                                            it.copy(
                                                bottomSheetType = BottomSheetType.Error(""),
                                                reservedBalance = null,
                                            )
                                        }
                                        if (_state.value.proDetails.isProPurchased) {
                                            aiVideoGenEventChannel.trySend(AiVideoGenEvent.RefreshProDetails)
                                        }
                                    }
                                }
                            },
                            failure = { error ->
                                uploadVideoTelemetry.aiVideoGenerated(
                                    model = _state.value.selectedProvider?.name ?: "",
                                    prompt = prompt,
                                    isSuccess = false,
                                    reason = error.message,
                                    reasonType = AiVideoGenFailureType.GENERATION_FAILED,
                                )
                                _state.update { it.copy(bottomSheetType = BottomSheetType.Error("")) }
                                if (_state.value.proDetails.isProPurchased) {
                                    aiVideoGenEventChannel.trySend(AiVideoGenEvent.RefreshProDetails)
                                }
                            },
                        )
                    }
            }
    }

    fun tryAgain() {
        viewModelScope.launch {
            _state.update { it.copy(bottomSheetType = BottomSheetType.None) }
            when {
                // If we have a request key, retry polling and uploading
                currentRequestKey != null && _state.value.reservedBalance != null -> {
                    pollAndUploadVideo(
                        modelName = _state.value.selectedProvider?.name ?: "",
                        prompt = _state.value.prompt.trim(),
                        requestKey = currentRequestKey!!,
                    )
                }
                // Otherwise, retry from the beginning (generate video)
                else -> {
                    generateAiVideo()
                }
            }
        }
    }

    fun updatePromptText(text: String) {
        _state.update { it.copy(prompt = text) }
    }

    fun selectProvider(provider: Provider) {
        _state.update { it.copy(selectedProvider = provider) }
        uploadVideoTelemetry.videoGenerationModelSelected(provider.name)
    }

    fun setBottomSheetType(type: BottomSheetType) {
        _state.update { it.copy(bottomSheetType = type) }
    }

    fun shouldEnableButton(): Boolean =
        with(_state.value) {
            prompt
                .trim()
                .isNotEmpty() &&
                usedCredits != null &&
                (isCreditsAvailable() || !isBalanceLow())
        }

    fun cleanup() {
        _state.update { current ->
            ViewState(
                isLoggedIn = current.isLoggedIn,
                proDetails = current.proDetails,
                isSubscriptionEnabled = current.isSubscriptionEnabled,
            )
        }
        currentRequestKey = null
        pollingJob?.cancel()
    }

    fun createAiVideoClicked() {
        with(_state.value) {
            selectedProvider?.name?.let {
                uploadVideoTelemetry.createAiVideoClicked(it, prompt)
            }
        }
    }

    fun resetUi() {
        val canister = _state.value.currentCanister
        cleanup()
        canister?.let { refresh(canister) }
    }

    fun updateBalance(balance: Long) {
        _state.update { it.copy(currentBalance = balance) }
    }

    suspend fun setSubscriptionNudgeShown(usedCredits: Int?) {
        if (usedCredits == 1 && !_state.value.proDetails.isProPurchased) {
            val todayEpochDays =
                Instant
                    .fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toEpochDays()
            val lastShownEpochDays =
                preferences.getLong(PrefKeys.AI_VIDEO_SUBSCRIPTION_NUDGE_LAST_SHOWN_DATE.name)
            if (lastShownEpochDays == null || lastShownEpochDays != todayEpochDays) {
                preferences.putLong(
                    PrefKeys.AI_VIDEO_SUBSCRIPTION_NUDGE_LAST_SHOWN_DATE.name,
                    todayEpochDays,
                )
                logger.d { "showSubscription Nudge" }
                aiVideoGenEventChannel.trySend(
                    AiVideoGenEvent.ShowSubscriptionNudge(
                        title = getString(Res.string.ai_video_subscription_nudge_title),
                        description =
                            getString(
                                Res.string.ai_video_subscription_nudge_description,
                                _state.value.proDetails.totalCredits,
                            ),
                    ),
                )
            }
        }
    }

    data class ViewState(
        val selectedProvider: Provider? = null,
        val providers: List<Provider> = emptyList(),
        val usedCredits: Int? = null,
        val totalCredits: Int = 1,
        val prompt: String = "",
        val uiState: UiState<String> = UiState.Initial,
        val bottomSheetType: BottomSheetType = BottomSheetType.None,
        val currentCanister: String? = null,
        val currentBalance: Long? = null,
        val reservedBalance: Long? = null,
        val isLoggedIn: Boolean = false,
        val proDetails: ProDetails = ProDetails(),
        val isSubscriptionEnabled: Boolean,
    ) {
        fun isBalanceLow() = (selectedProvider?.cost?.sats ?: 0) > (currentBalance ?: -1)

        fun isCreditsAvailable() = (usedCredits ?: 1) < totalCredits
    }

    sealed class BottomSheetType {
        data object None : BottomSheetType()
        data object ModelSelection : BottomSheetType()
        data class Error(
            val message: String,
            val endFlow: Boolean = false,
        ) : BottomSheetType()
        data object BackConfirmation : BottomSheetType()
    }

    internal data class RequiredUseCases(
        val getProviders: GetProvidersUseCase,
        val getFreeCreditsStatus: GetFreeCreditsStatusUseCase,
        val generateVideo: GenerateVideoUseCase,
        val pollAndUploadAiVideo: PollAndUploadAiVideoUseCase,
    )

    sealed class AiVideoGenEvent {
        data class ShowSubscriptionNudge(
            val title: String,
            val description: String,
            val entryPoint: SubscriptionEntryPoint = SubscriptionEntryPoint.AI_VIDEO,
        ) : AiVideoGenEvent()
        data object RefreshProDetails : AiVideoGenEvent()
    }
}
