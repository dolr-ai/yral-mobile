package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.events.AiVideoGenFailureType
import com.yral.shared.analytics.events.VideoCreationType
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.uploadvideo.analytics.UploadVideoTelemetry
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetFreeCreditsStatusUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.PollAndUploadAiVideoUseCase
import com.yral.shared.features.uploadvideo.domain.VideoGenerationTimeoutException
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.uniffi.generated.RateLimitStatusWrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiVideoGenViewModel internal constructor(
    authClientFactory: AuthClientFactory,
    private val requiredUseCases: RequiredUseCases,
    private val sessionManager: SessionManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val uploadVideoTelemetry: UploadVideoTelemetry,
    logger: YralLogger,
) : ViewModel() {
    private val logger = logger.withTag(AiVideoGenViewModel::class.simpleName ?: "")

    private val authClient =
        authClientFactory
            .create(viewModelScope) { e ->
                logger.e(e) { "Auth error" }
                handleSignupFailed()
            }

    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()
    val sessionObserver =
        combine(
            sessionManager.state,
            sessionManager.observeSessionProperties(),
        ) { state, properties ->
            // required to dismiss sheet since viewModel is not be recreated
            if (_state.value.bottomSheetType is BottomSheetType.Signup) {
                _state.update { it.copy(bottomSheetType = BottomSheetType.None) }
            }
            when (state) {
                is SessionState.SignedIn -> state.session.canisterId
                else -> null
            }
        }.distinctUntilChanged()

    private var currentRequestKey: VideoGenRequestKeyWrapper? = null
    private var pollingJob: Job? = null

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
                                isRegistered = sessionManager.isSocialSignIn(),
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

    private fun RateLimitStatusWrapper.usedCredits() =
        if (sessionManager.isSocialSignIn()) {
            if (isLimited) 1 else 0
        } else {
            0
        }

    fun generateAiVideo() {
        if (!sessionManager.isSocialSignIn()) {
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
                                            generateAudio = true,
                                            tokenType = "Free",
                                            userId = userId,
                                        ),
                                ),
                        ).onSuccess { result ->
                            logger.d { "Video generated: $result" }
                            result.requestKey?.let { requestKey ->
                                currentRequestKey = requestKey
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

    private fun pollAndUploadVideo(
        modelName: String,
        requestKey: VideoGenRequestKeyWrapper,
    ) {
        pollingJob?.cancel()
        pollingJob =
            viewModelScope.launch {
                val canisterID = sessionManager.canisterID ?: return@launch
                try {
                    requiredUseCases
                        .pollAndUploadAiVideo
                        .invoke(
                            parameters =
                                PollAndUploadAiVideoUseCase.Params(
                                    canisterID = canisterID,
                                    modelName = modelName,
                                    requestKey = requestKey,
                                    isFastInitially = false,
                                    hashtags = emptyList(),
                                    description = "",
                                    isNsfw = false,
                                    enableHotOrNot = false,
                                ),
                        ).collect { result ->
                            when (val pollResult = result.value) {
                                is PollAndUploadAiVideoUseCase.PollAndUploadResult.InProgress -> {
                                    _state.update { it.copy(uiState = UiState.InProgress(0f)) }
                                }
                                is PollAndUploadAiVideoUseCase.PollAndUploadResult.Success -> {
                                    logger.d { "Generated video uploaded successfully" }
                                    _state.update { it.copy(uiState = UiState.Success(pollResult.videoUrl)) }
                                }
                                is PollAndUploadAiVideoUseCase.PollAndUploadResult.Failed -> {
                                    _state.update {
                                        it.copy(bottomSheetType = BottomSheetType.Error(pollResult.errorMessage, true))
                                    }
                                }
                            }
                        }
                } catch (e: VideoGenerationTimeoutException) {
                    logger.e { e.message }
                    _state.update { it.copy(bottomSheetType = BottomSheetType.Error("")) }
                } catch (e: CancellationException) {
                    logger.e(e) { "Error polling and uploading video" }
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    logger.e(e) { "Error polling and uploading video" }
                    _state.update { it.copy(bottomSheetType = BottomSheetType.Error("")) }
                }
            }
    }

    fun tryAgain() {
        viewModelScope.launch {
            _state.update { it.copy(bottomSheetType = BottomSheetType.None) }
            when {
                // If we have a request key, retry polling and uploading
                currentRequestKey != null -> {
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

    fun shouldEnableButton(): Boolean {
        val currentState = _state.value
        return currentState.prompt.trim().isNotEmpty() &&
            currentState.usedCredits != null &&
            currentState.usedCredits < currentState.totalCredits
    }

    fun cleanup() {
        _state.update { ViewState() }
        currentRequestKey = null
        pollingJob?.cancel()
    }

    @Suppress("TooGenericExceptionCaught")
    fun signInWithGoogle(context: Any) {
        viewModelScope.launch {
            try {
                authClient.signInWithSocial(context, SocialProvider.GOOGLE)
            } catch (e: Exception) {
                crashlyticsManager.recordException(e)
                handleSignupFailed()
            }
        }
    }

    private fun handleSignupFailed() {
        setBottomSheetType(type = BottomSheetType.SignupFailed)
    }

    fun createAiVideoClicked() {
        _state.value.selectedProvider
            ?.name
            ?.let { uploadVideoTelemetry.createAiVideoClicked(it) }
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
    )

    sealed class BottomSheetType {
        data object None : BottomSheetType()
        data object ModelSelection : BottomSheetType()
        data class Error(
            val message: String,
            val endFlow: Boolean = false,
        ) : BottomSheetType()
        data object Signup : BottomSheetType()
        data class Link(
            val url: String,
        ) : BottomSheetType()
        data object SignupFailed : BottomSheetType()
        data object BackConfirmation : BottomSheetType()
    }

    internal data class RequiredUseCases(
        val getProviders: GetProvidersUseCase,
        val getFreeCreditsStatus: GetFreeCreditsStatusUseCase,
        val generateVideo: GenerateVideoUseCase,
        val pollAndUploadAiVideo: PollAndUploadAiVideoUseCase,
    )
}
