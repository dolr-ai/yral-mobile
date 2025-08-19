package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetFreeCreditsStatusUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.PollGenerationStatusUseCase
import com.yral.shared.features.uploadvideo.domain.UploadAiVideoFromUrlUseCase
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.uniffi.generated.VideoGenRequestKey
import com.yral.shared.uniffi.generated.VideoGenRequestStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiVideoGenViewModel internal constructor(
    private val requiredUseCases: RequiredUseCases,
    private val sessionManager: SessionManager,
    logger: YralLogger,
) : ViewModel() {
    private val logger = logger.withTag(AiVideoGenViewModel::class.simpleName ?: "")

    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private var currentRequestKey: VideoGenRequestKey? = null
    private var currentVideoUrl: String? = null

    fun initialize() {
        if (_state.value.uiState is UiState.Initial) {
            _state.update { it.copy(usedCredits = null) }
            getFreeCreditsStatus()
            loadProviders()
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
            requiredUseCases
                .getFreeCreditsStatus()
                .onSuccess { status ->
                    _state.update { it.copy(usedCredits = if (status.isLimited) 1 else 0) }
                    logger.d { "Used credits ${_state.value.usedCredits} $status" }
                }.onFailure { error ->
                    logger.e(error) { "Error fetching free credits" }
                    _state.update { it.copy(usedCredits = null) }
                }
        }
    }

    fun generateAiVideo() {
        viewModelScope.launch {
            val currentState = _state.value
            currentState.selectedProvider?.let { selectedProvider ->
                sessionManager.userPrincipal?.let { userId ->
                    _state.update { it.copy(uiState = UiState.InProgress(0f)) }
                    currentRequestKey = null
                    currentVideoUrl = null
                    requiredUseCases
                        .generateVideo(
                            parameter =
                                GenerateVideoUseCase.Param(
                                    params =
                                        GenerateVideoParams(
                                            providerId = selectedProvider.id,
                                            prompt = currentState.prompt,
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
                                pollGeneration(requestKey)
                            }
                        }.onFailure { error ->
                            logger.e(error) { "Error generating video" }
                            _state.update {
                                it.copy(bottomSheetType = BottomSheetType.Error(error.message ?: ""))
                            }
                        }
                }
            }
        }
    }

    private suspend fun pollGeneration(requestKey: VideoGenRequestKey) {
        try {
            requiredUseCases
                .pollGenerationStatusUseCase
                .invoke(
                    parameters =
                        PollGenerationStatusUseCase.Params(
                            requestKey = requestKey,
                            isFastInitially = false,
                        ),
                ).collect { collectedStatus ->
                    logger.d { "Video generation status: ${collectedStatus.value}" }
                    when (val status = collectedStatus.value) {
                        is VideoGenRequestStatus.Complete -> {
                            currentVideoUrl = status.v1
                            uploadAiVideoFromUrl(status.v1)
                        }
                        VideoGenRequestStatus.Pending,
                        VideoGenRequestStatus.Processing,
                        -> {
                            _state.update { it.copy(uiState = UiState.InProgress(0f)) }
                        }
                        is VideoGenRequestStatus.Failed -> {
                            _state.update {
                                it.copy(bottomSheetType = BottomSheetType.Error(status.v1))
                            }
                        }
                    }
                }
        } catch (e: CancellationException) {
            logger.e(e) { "Error polling generation status" }
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            logger.e(e) { "Error polling generation status" }
            _state.update {
                it.copy(bottomSheetType = BottomSheetType.Error("", false))
            }
        }
    }

    private suspend fun uploadAiVideoFromUrl(videoUrl: String) {
        try {
            logger.d { "Triggered upload_ai_video_from_url for $videoUrl" }
            requiredUseCases.uploadAiVideoFromUrl.invoke(
                parameter =
                    UploadAiVideoFromUrlUseCase.Params(
                        videoUrl = videoUrl,
                        hashtags = emptyList(),
                        description = "",
                        isNsfw = false,
                        enableHotOrNot = false,
                    ),
            )
            logger.d { "Generated video uploaded successfully" }
            _state.update { it.copy(uiState = UiState.Success(videoUrl)) }
        } catch (e: CancellationException) {
            logger.e(e) { "Error updating metadata for video" }
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            logger.e(e) { "Error calling upload_ai_video_from_url" }
            _state.update {
                it.copy(bottomSheetType = BottomSheetType.Error("", false))
            }
        }
    }

    fun tryAgain() {
        viewModelScope.launch {
            _state.update { it.copy(bottomSheetType = BottomSheetType.None) }
            when {
                // If we have a video URL, retry upload
                currentVideoUrl != null -> {
                    uploadAiVideoFromUrl(currentVideoUrl!!)
                }
                // If we have a request key, retry polling
                currentRequestKey != null -> {
                    pollGeneration(currentRequestKey!!)
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
    }

    fun setBottomSheetType(type: BottomSheetType) {
        _state.update { it.copy(bottomSheetType = type) }
    }

    fun shouldEnableButton(): Boolean {
        val currentState = _state.value
        return currentState.prompt.isNotEmpty() &&
            currentState.usedCredits != null &&
            currentState.usedCredits < currentState.totalCredits
    }

    fun cleanup() {
        _state.update { ViewState() }
        currentRequestKey = null
        currentVideoUrl = null
    }

    data class ViewState(
        val selectedProvider: Provider? = null,
        val providers: List<Provider> = emptyList(),
        val usedCredits: Int? = null,
        val totalCredits: Int = 1,
        val prompt: String = "",
        val uiState: UiState<String> = UiState.Initial,
        val bottomSheetType: BottomSheetType = BottomSheetType.None,
    )

    sealed class BottomSheetType {
        data object None : BottomSheetType()
        data object ModelSelection : BottomSheetType()
        data class Error(
            val message: String,
            val endFlow: Boolean = false,
        ) : BottomSheetType()
    }

    internal data class RequiredUseCases(
        val getProviders: GetProvidersUseCase,
        val getFreeCreditsStatus: GetFreeCreditsStatusUseCase,
        val generateVideo: GenerateVideoUseCase,
        val pollGenerationStatusUseCase: PollGenerationStatusUseCase,
        val uploadAiVideoFromUrl: UploadAiVideoFromUrlUseCase,
    )
}
