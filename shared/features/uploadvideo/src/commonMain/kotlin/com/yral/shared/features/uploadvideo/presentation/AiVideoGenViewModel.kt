package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.accountFeatureFlags.AccountFeatureFlags
import com.yral.shared.analytics.events.AiVideoGenFailureType
import com.yral.shared.analytics.events.VideoCreationType
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.uploadvideo.analytics.UploadVideoTelemetry
import com.yral.shared.features.uploadvideo.data.remote.models.TokenType
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetFreeCreditsStatusUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.PollAndUploadAiVideoUseCase
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.rust.service.domain.models.RateLimitStatus
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class AiVideoGenViewModel internal constructor(
    private val requiredUseCases: RequiredUseCases,
    private val sessionManager: SessionManager,
    private val uploadVideoTelemetry: UploadVideoTelemetry,
    private val flagManager: FeatureFlagManager,
    logger: YralLogger,
) : ViewModel() {
    private val logger = logger.withTag(AiVideoGenViewModel::class.simpleName ?: "")

    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()
    val sessionObserver =
        sessionManager.observeSessionStateWithProperty { state, properties ->
            if (_state.value.bottomSheetType is BottomSheetType.Signup) {
                _state.update { it.copy(bottomSheetType = BottomSheetType.None) }
            }
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
            sessionManager.observeSessionPropertyWithDefault(
                selector = { it.isSocialSignIn },
                defaultValue = false,
            ) { isSocialSignIn ->
                _state.update { it.copy(isLoggedIn = isSocialSignIn) }
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
            _state.update { it.copy(usedCredits = null) }
            sessionManager.userPrincipal?.let { userPrincipal ->
                requiredUseCases
                    .getFreeCreditsStatus(
                        parameter =
                            GetFreeCreditsStatusUseCase.Params(
                                userPrincipal = userPrincipal,
                                isRegistered = _state.value.isLoggedIn,
                            ),
                    ).onSuccess { status ->
                        uploadVideoTelemetry.videoCreationPageViewed(
                            type = VideoCreationType.AI_VIDEO,
                            creditsFetched = true,
                            creditsAvailable = 1 - status.usedCredits(),
                        )
                        _state.update { it.copy(usedCredits = status.usedCredits()) }
                        logger.d { "Used credits ${_state.value.usedCredits} $status" }
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

    fun generateAiVideo() {
        if (!_state.value.isLoggedIn) {
            setBottomSheetType(type = BottomSheetType.Signup)
            return
        }
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
                                                    TokenType.FREE
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
                                    requestKey = requestKey,
                                )
                                return@onSuccess
                            }
                            result.providerError?.let { error ->
                                pushTriggerFailed(selectedProvider.name, error)
                                _state.update {
                                    it.copy(bottomSheetType = BottomSheetType.Error(error, true))
                                }
                            }
                        }.onFailure { error ->
                            logger.e(error) { "Error generating video" }
                            pushTriggerFailed(selectedProvider.name, error.message ?: "")
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
        reason: String,
    ) {
        uploadVideoTelemetry.aiVideoGenerated(
            model = model,
            isSuccess = false,
            reason = reason,
            reasonType = AiVideoGenFailureType.TRIGGER_FAILED,
        )
    }

    @Suppress("LongMethod")
    private fun pollAndUploadVideo(
        modelName: String,
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
                                    }

                                    is PollAndUploadAiVideoUseCase.PollAndUploadResult.UploadFailed -> {
                                        _state.update {
                                            it.copy(
                                                bottomSheetType = BottomSheetType.Error(""),
                                                reservedBalance = null,
                                            )
                                        }
                                    }
                                }
                            },
                            failure = { error ->
                                uploadVideoTelemetry.aiVideoGenerated(
                                    model = _state.value.selectedProvider?.name ?: "",
                                    isSuccess = false,
                                    reason = error.message,
                                    reasonType = AiVideoGenFailureType.GENERATION_FAILED,
                                )
                                _state.update { it.copy(bottomSheetType = BottomSheetType.Error("")) }
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
        _state.update { ViewState() }
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

    fun getTncLink(): String = flagManager.get(AccountFeatureFlags.AccountLinks.Links).tnc

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
        data object Signup : BottomSheetType()
        data object BackConfirmation : BottomSheetType()
    }

    internal data class RequiredUseCases(
        val getProviders: GetProvidersUseCase,
        val getFreeCreditsStatus: GetFreeCreditsStatusUseCase,
        val generateVideo: GenerateVideoUseCase,
        val pollAndUploadAiVideo: PollAndUploadAiVideoUseCase,
    )
}
