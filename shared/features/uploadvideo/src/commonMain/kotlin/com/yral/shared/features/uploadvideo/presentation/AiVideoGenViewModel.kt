package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.yral.shared.core.videostate.VideoGenerationTracker
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.subscriptions.analytics.SubscriptionTelemetry
import com.yral.shared.features.uploadvideo.analytics.UploadVideoTelemetry
import com.yral.shared.features.uploadvideo.data.remote.models.TokenType
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoErrorType
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.ImageData
import com.yral.shared.features.uploadvideo.domain.models.ImageInput
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class, ExperimentalEncodingApi::class)
@Suppress("TooManyFunctions")
class AiVideoGenViewModel internal constructor(
    private val requiredUseCases: RequiredUseCases,
    private val sessionManager: SessionManager,
    @Suppress("UnusedPrivateProperty") private val preferences: Preferences,
    private val uploadVideoTelemetry: UploadVideoTelemetry,
    private val subscriptionTelemetry: SubscriptionTelemetry,
    private val videoDraftPollingManager: VideoDraftPollingManager,
    private val crashlyticsManager: CrashlyticsManager,
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

    private val aiVideoGenEventChannel = Channel<AiVideoGenEvent>(Channel.BUFFERED)
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

    init {
        loadProviders()
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

            else -> {
                logger.d { "Same canister" }
            }
        }
        when (_state.value.uiState) {
            is UiState.Initial -> {
                _state.value.currentCanister?.let {
                    if (_state.value.providers.isEmpty()) {
                        loadProviders()
                    }
                }
            }

            is UiState.InProgress -> {
                if (isCanisterChanged) {
                    logger.d { "Canister changed, cancelling polling" }
                    cleanup()
                    loadProviders()
                } else {
                    logger.d { "Canister unchanged, reusing polling" }
                }
            }

            else -> {
                Unit
            }
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

    @Suppress("LongMethod")
    fun generateAiVideo() {
        viewModelScope.launch {
            val currentState = _state.value
            if (!currentState.hasRequiredGenerationInput()) {
                return@launch
            }
            currentState.selectedProvider?.let { selectedProvider ->
                sessionManager.userPrincipal?.let { userId ->
                    _state.update { it.copy(uiState = UiState.InProgress(0f)) }
                    VideoGenerationTracker.startGenerating()
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
                                            image = currentState.toImageData(),
                                            tokenType =
                                                if (currentState.proDetails.isProPurchased) {
                                                    TokenType.YRAL_PRO_SUBSCRIPTION
                                                } else {
                                                    TokenType.FREE
                                                },
                                            userId = userId,
                                            uploadHandling = SERVER_DRAFT,
                                        ),
                                ),
                        ).onSuccess { result ->
                            logger.d { "Video generated: $result" }
                            result.providerError?.let { error ->
                                VideoGenerationTracker.stopGenerating()
                                crashlyticsManager.recordException(Exception(error), ExceptionType.AI_VIDEO)
                                pushTriggerFailed(
                                    model = selectedProvider.name,
                                    prompt = currentState.prompt.trim(),
                                    reason = error,
                                )
                                _state.update {
                                    it.copy(
                                        uiState = UiState.Initial,
                                        bottomSheetType =
                                            BottomSheetType.Error(
                                                title = result.errorType,
                                                message = error,
                                                endFlow = true,
                                            ),
                                    )
                                }
                                return@onSuccess
                            }
                            // Server handles the long-running generation and draft creation.
                            // Keep the tracker active until the draft-created notification arrives.
                            uploadVideoTelemetry.aiVideoRequestSubmitted(
                                model = selectedProvider.name,
                                prompt = currentState.prompt.trim(),
                                isSuccess = true,
                                reason = null,
                                reasonType = null,
                            )
                            videoDraftPollingManager.onGenerationSubmitted(userId)
                            _state.update {
                                it.copy(
                                    uiState = UiState.Initial,
                                )
                            }
                            aiVideoGenEventChannel.trySend(AiVideoGenEvent.ShowGeneratedToast)
                            aiVideoGenEventChannel.trySend(AiVideoGenEvent.NavigateToHome)
                            if (currentState.proDetails.isProPurchased) {
                                val creditsRemaining =
                                    currentState.proDetails.availableCredits - 1
                                subscriptionTelemetry.onCreditsConsumed(
                                    feature = CreditFeature.AI_VIDEO,
                                    creditsUsed = 1,
                                    creditsRemaining = creditsRemaining.coerceAtLeast(0),
                                )
                                aiVideoGenEventChannel.trySend(AiVideoGenEvent.RefreshProDetails)
                            }
                        }.onFailure { error ->
                            VideoGenerationTracker.stopGenerating()
                            logger.e(error) { "Error generating video" }
                            pushTriggerFailed(
                                model = selectedProvider.name,
                                prompt = currentState.prompt.trim(),
                                reason = error.message ?: "",
                            )
                            _state.update {
                                it.copy(
                                    uiState = UiState.Initial,
                                    bottomSheetType = BottomSheetType.Error(message = "", endFlow = true),
                                )
                            }
                        }
                }
            }
        }
    }

    private fun pushTriggerFailed(
        model: String,
        prompt: String,
        reason: String,
    ) {
        uploadVideoTelemetry.aiVideoRequestSubmitted(
            model = model,
            prompt = prompt,
            isSuccess = false,
            reason = reason,
            reasonType = AiVideoGenFailureType.TRIGGER_FAILED,
        )
    }

    fun tryAgain() {
        viewModelScope.launch {
            _state.update { it.copy(bottomSheetType = BottomSheetType.None) }
            generateAiVideo()
        }
    }

    fun updatePromptText(text: String) {
        _state.update { it.copy(prompt = text) }
    }

    fun updateGenerationMode(mode: AiVideoGenerationMode) {
        _state.update { it.copy(generationMode = mode) }
    }

    fun updateSelectedImage(bytes: ByteArray) {
        _state.update {
            it.copy(
                selectedImageBytes = bytes,
                selectedImageMimeType = resolveImageMimeType(bytes),
            )
        }
    }

    fun clearSelectedImage() {
        _state.update {
            it.copy(
                selectedImageBytes = null,
                selectedImageMimeType = null,
            )
        }
    }

    fun setBottomSheetType(type: BottomSheetType) {
        _state.update { it.copy(bottomSheetType = type) }
    }

    fun shouldEnableButton(): Boolean = _state.value.shouldEnableButton()

    fun cleanup() {
        _state.update { current ->
            ViewState(
                isLoggedIn = current.isLoggedIn,
                proDetails = current.proDetails,
                isSubscriptionEnabled = current.isSubscriptionEnabled,
            )
        }
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

    data class ViewState(
        val selectedProvider: Provider? = null,
        val providers: List<Provider> = emptyList(),
        val prompt: String = "",
        val generationMode: AiVideoGenerationMode = AiVideoGenerationMode.IMAGE_TO_VIDEO,
        val selectedImageBytes: ByteArray? = null,
        val selectedImageMimeType: String? = null,
        val uiState: UiState<String> = UiState.Initial,
        val bottomSheetType: BottomSheetType = BottomSheetType.None,
        val currentCanister: String? = null,
        val currentBalance: Long? = null,
        val isLoggedIn: Boolean = false,
        val proDetails: ProDetails = ProDetails(),
        val isSubscriptionEnabled: Boolean,
    ) {
        fun isBalanceLow() = (selectedProvider?.cost?.sats ?: 0) > (currentBalance ?: -1)

        fun shouldEnableButton(): Boolean = hasRequiredGenerationInput()

        fun hasRequiredGenerationInput(): Boolean =
            prompt.trim().isNotEmpty() &&
                selectedProvider != null &&
                when (generationMode) {
                    AiVideoGenerationMode.TEXT_TO_VIDEO -> {
                        true
                    }

                    AiVideoGenerationMode.IMAGE_TO_VIDEO -> {
                        selectedProvider.supportsImage == true &&
                            selectedImageBytes != null &&
                            selectedImageMimeType != null
                    }
                }

        fun toImageData(): ImageData? {
            val bytes = selectedImageBytes
            val mimeType = selectedImageMimeType
            return if (generationMode == AiVideoGenerationMode.IMAGE_TO_VIDEO && bytes != null && mimeType != null) {
                ImageData.Base64(
                    ImageInput(
                        data = Base64.Default.encode(bytes),
                        mimeType = mimeType,
                    ),
                )
            } else {
                null
            }
        }
    }

    sealed class BottomSheetType {
        data object None : BottomSheetType()
        data class Error(
            val title: GenerateVideoErrorType? = null,
            val message: String,
            val endFlow: Boolean = false,
        ) : BottomSheetType()
    }

    internal data class RequiredUseCases(
        val getProviders: GetProvidersUseCase,
        val generateVideo: GenerateVideoUseCase,
    )

    private companion object {
        const val SERVER_DRAFT = "ServerDraft"
        private val PNG_MAGIC =
            byteArrayOf(
                0x89.toByte(),
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A,
            )
        private val JPEG_MAGIC =
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(),
                0xFF.toByte(),
            )
        private const val MIME_TYPE_PNG = "image/png"
        private const val MIME_TYPE_JPEG = "image/jpeg"

        private fun resolveImageMimeType(bytes: ByteArray): String {
            val isPng =
                bytes.size >= PNG_MAGIC.size &&
                    bytes.copyOfRange(0, PNG_MAGIC.size).contentEquals(PNG_MAGIC)
            val isJpeg =
                bytes.size >= JPEG_MAGIC.size &&
                    bytes.copyOfRange(0, JPEG_MAGIC.size).contentEquals(JPEG_MAGIC)
            return when {
                isPng -> MIME_TYPE_PNG
                isJpeg -> MIME_TYPE_JPEG
                else -> MIME_TYPE_PNG
            }
        }
    }

    sealed class AiVideoGenEvent {
        data object RefreshProDetails : AiVideoGenEvent()
        data object ShowGeneratedToast : AiVideoGenEvent()
        data object NavigateToHome : AiVideoGenEvent()
    }
}

enum class AiVideoGenerationMode {
    TEXT_TO_VIDEO,
    IMAGE_TO_VIDEO,
}
