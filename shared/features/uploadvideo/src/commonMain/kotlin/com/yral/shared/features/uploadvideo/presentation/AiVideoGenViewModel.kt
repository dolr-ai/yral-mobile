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
import kotlinx.coroutines.launch

class AiVideoGenViewModel internal constructor(
    private val requiredUseCases: RequiredUseCases,
    private val sessionManager: SessionManager,
    logger: YralLogger,
) : ViewModel() {
    private val logger = logger.withTag(AiVideoGenViewModel::class.simpleName ?: "")

    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    init {
        loadProviders()
        getFreeCreditsStatus()
    }

    fun loadProviders() {
        viewModelScope.launch {
            requiredUseCases
                .getProviders()
                .onSuccess { list -> _state.value = _state.value.copy(providers = list) }
                .onFailure { error ->
                    logger.e(error) { "Error fetching providers" }
                    _state.value = _state.value.copy(providers = emptyList())
                }
        }
    }

    fun getFreeCreditsStatus() {
        viewModelScope.launch {
            requiredUseCases
                .getFreeCreditsStatus()
                .onSuccess { status ->
                    _state.value = _state.value.copy(availableCredits = if (status.isLimited) 0 else 1)
                }.onFailure { error ->
                    logger.e(error) { "Error fetching free credits" }
                    _state.value = _state.value.copy(availableCredits = 0)
                }
        }
    }

    fun generateAiVideo() {
        viewModelScope.launch {
            sessionManager.userPrincipal?.let { userId ->
                _state.value = _state.value.copy(generationInProgress = UiState.InProgress(0f))
                requiredUseCases
                    .generateVideo(
                        parameter =
                            GenerateVideoUseCase.Param(
                                params =
                                    GenerateVideoParams(
                                        providerId = "veo3",
                                        prompt = "Testing AI Video gen api",
                                        aspectRatio = "16:9",
                                        durationSeconds = 8,
                                        generateAudio = true,
                                        tokenType = "Free",
                                        userId = userId,
                                    ),
                            ),
                    ).onSuccess { result ->
                        logger.d { "Video generated: $result" }
                        result.requestKey?.let { pollGeneration(it) }
                    }.onFailure { error ->
                        logger.e(error) { "Error generating video" }
                        _state.value = _state.value.copy(generationInProgress = UiState.Failure(error))
                    }
            }
        }
    }

    suspend fun pollGeneration(requestKey: VideoGenRequestKey) {
        try {
            requiredUseCases
                .pollGenerationStatusUseCase
                .invoke(
                    parameters =
                        PollGenerationStatusUseCase.Params(
                            requestKey = requestKey,
                            isFastInitially = false,
                        ),
                ).collect {
                    logger.d { "Video generation status: ${it.value}" }
                    val status = it.value
                    if (status is VideoGenRequestStatus.Processing || status is VideoGenRequestStatus.Pending) {
                        _state.value = _state.value.copy(generationInProgress = UiState.InProgress(0f))
                    }
                    if (status is VideoGenRequestStatus.Complete) {
                        _state.value = _state.value.copy(generationInProgress = UiState.Success(Unit))
                        uploadAiVideoFromUrl(status.v1)
                    }
                }
        } catch (e: CancellationException) {
            logger.e(e) { "Error polling generation status" }
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            logger.e(e) { "Error polling generation status" }
            _state.value = _state.value.copy(generationInProgress = UiState.Failure(e))
        }
    }

    private suspend fun uploadAiVideoFromUrl(videoUrl: String) {
        try {
            logger.d { "Triggered upload_ai_video_from_url for $videoUrl" }
            _state.value = _state.value.copy(uploadAiVideoUiState = UiState.InProgress(0f))
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
            _state.value = _state.value.copy(uploadAiVideoUiState = UiState.Success(Unit))
        } catch (e: CancellationException) {
            logger.e(e) { "Error polling generation status" }
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            logger.e(e) { "Error calling upload_ai_video_from_url" }
            _state.value = _state.value.copy(uploadAiVideoUiState = UiState.Failure(e))
        }
    }

    data class ViewState(
        val providers: List<Provider> = emptyList(),
        val availableCredits: Int = 0,
        val generationInProgress: UiState<Unit> = UiState.Initial,
        val uploadAiVideoUiState: UiState<Unit> = UiState.Initial,
    )

    internal data class RequiredUseCases(
        val getProviders: GetProvidersUseCase,
        val getFreeCreditsStatus: GetFreeCreditsStatusUseCase,
        val generateVideo: GenerateVideoUseCase,
        val pollGenerationStatusUseCase: PollGenerationStatusUseCase,
        val uploadAiVideoFromUrl: UploadAiVideoFromUrlUseCase,
    )
}
