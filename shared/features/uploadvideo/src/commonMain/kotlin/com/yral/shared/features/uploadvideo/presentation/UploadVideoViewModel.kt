package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.uploadvideo.analytics.UploadVideoTelemetry
import com.yral.shared.features.uploadvideo.domain.GetUploadEndpointUseCase
import com.yral.shared.features.uploadvideo.domain.UpdateMetaUseCase
import com.yral.shared.features.uploadvideo.domain.UploadVideoUseCase
import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadState
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

@Suppress("TooManyFunctions")
class UploadVideoViewModel internal constructor(
    private val getUploadEndpoint: GetUploadEndpointUseCase,
    private val uploadVideo: UploadVideoUseCase,
    private val updateMeta: UpdateMetaUseCase,
    private val appDispatchers: AppDispatchers,
    private val uploadVideoTelemetry: UploadVideoTelemetry,
    private val crashlyticsManager: CrashlyticsManager,
    logger: YralLogger,
) : ViewModel() {
    private val logger = logger.withTag(UploadVideoViewModel::class.simpleName ?: "")

    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = eventChannel.receiveAsFlow()

    private var backgroundUploadJob: Job? = null
    private var completeProcessJob: Job? = null

    private fun send(event: Event) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    fun onFileSelected(filePath: String) {
        if (filePath.isBlank()) {
            // User deselected the video, cancel and cleanup
            cleanup()
            resetUploadStates()
            _state.update { it.copy(selectedFilePath = null) }
        } else {
            _state.update { it.copy(selectedFilePath = filePath) }
            uploadVideoTelemetry.fileSelected()
            startBackgroundUpload(filePath)
        }
    }

    fun onCaptionChanged(caption: String) {
        _state.update { it.copy(caption = caption) }
    }

    fun onHashtagsChanged(hashtags: List<String>) {
        _state.update { it.copy(hashtags = hashtags) }
    }

    fun onUploadButtonClicked() {
        uploadVideoTelemetry.uploadInitiated()
        validateAndPublish()
    }

    fun onRetryClicked() {
        _state.update { it.copy(errorAnalyticsPushed = false) }
        validateAndPublish()
    }

    fun onGoToHomeClicked() {
        resetState()
        send(Event.GoToHome)
    }

    private fun startBackgroundUpload(filePath: String) {
        cancelUpload()
        resetUploadStates()

        backgroundUploadJob =
            viewModelScope.launch {
                performBackgroundUpload(filePath)
            }
    }

    private suspend fun performBackgroundUpload(filePath: String) {
        try {
            log { "performBackgroundUpload" }
            _state.update {
                it.copy(fileUploadUiState = UiState.InProgress(0f))
            }
            // Get upload endpoint
            val endpointResult = getUploadEndpoint()
            val endpoint = endpointResult.getOrThrow()

            log { "performBackgroundUpload endpoint: $endpoint" }

            // Start video upload
            uploadVideo(UploadVideoUseCase.Params(endpoint.url, filePath))
                .collect { result ->
                    val fileUploadUiState =
                        result.fold(
                            success = { uploadState ->
                                when (uploadState) {
                                    is UploadState.Uploaded -> UiState.Success(endpoint)
                                    is UploadState.InProgress -> {
                                        val progress =
                                            if (uploadState.totalBytes != null) {
                                                uploadState.bytesSent.toFloat() / uploadState.totalBytes.toFloat()
                                            } else {
                                                0f
                                            }
                                        UiState.InProgress(progress)
                                    }
                                }
                            },
                            failure = { error -> UiState.Failure(error) },
                        )
                    _state.update { it.copy(fileUploadUiState = fileUploadUiState) }
                }

            log { "performBackgroundUpload done" }
        } catch (e: CancellationException) {
            log { "performBackgroundUpload cancelled" }
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            log { "performBackgroundUpload error" }
            _state.update { it.copy(fileUploadUiState = UiState.Failure(e)) }
        }
    }

    @Suppress("ReturnCount")
    private fun validateAndPublish() {
        log { "validateAndPublish" }
        val currentState = _state.value

        // Validate inputs
        if (currentState.selectedFilePath.isNullOrBlank()) {
            send(Event.ShowInputError.SelectFile)
            return
        }

        if (currentState.caption.isBlank()) {
            send(Event.ShowInputError.AddCaption)
            return
        }

        if (currentState.hashtags.isEmpty()) {
            send(Event.ShowInputError.AddHashtags)
            return
        }

        // Check if we're already in the middle of the complete process
        if (currentState.uploadUiState is UiState.InProgress) {
            return
        }

        startCompletePublishProcess(
            currentState.selectedFilePath,
            currentState.caption,
            currentState.hashtags,
        )
    }

    private fun startCompletePublishProcess(
        filePath: String,
        caption: String,
        hashtags: List<String>,
    ) {
        // Cancel any existing complete process job
        completeProcessJob?.cancel()

        completeProcessJob =
            viewModelScope.launch {
                _state.update { it.copy(updateMetadataUiState = UiState.InProgress(0f)) }
                try {
                    // Ensure file upload is completed before proceeding
                    ensureFileUploadCompleted(filePath)

                    // Wait for upload completion and update metadata
                    waitForUploadCompletionAndUpdateMetadata(caption, hashtags)
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    _state.update { it.copy(updateMetadataUiState = UiState.Failure(e)) }
                    send(Event.UploadFailed(e))
                }
            }
    }

    private suspend fun ensureFileUploadCompleted(filePath: String) {
        when (state.value.fileUploadUiState) {
            is UiState.Failure -> {
                // File upload failed, retry upload first
                performBackgroundUpload(filePath)
            }

            UiState.Initial -> {
                // No upload started, start fresh
                performBackgroundUpload(filePath)
            }

            is UiState.Success -> {
                // File upload already completed, proceed
                return
            }

            is UiState.InProgress -> {
                // File upload in progress, wait for completion
                // This will be handled by waitForUploadCompletionAndUpdateMetadata
            }
        }
    }

    private suspend fun waitForUploadCompletionAndUpdateMetadata(
        caption: String,
        hashtags: List<String>,
    ) {
        log { "Waiting for upload completion" }

        // Monitor file upload state until completion using Flow collection
        state
            .map { it.fileUploadUiState }
            .distinctUntilChanged()
            .collect { fileUploadState ->
                when (fileUploadState) {
                    is UiState.Success -> {
                        // File upload completed, now start metadata update
                        updateMetadata(fileUploadState.data, caption, hashtags)
                        return@collect
                    }

                    is UiState.Failure -> {
                        throw fileUploadState.error
                    }

                    is UiState.InProgress -> {
                        // Continue waiting for completion
                    }

                    UiState.Initial -> {
                        // This shouldn't happen if we properly ensured upload started
                        error("Upload state reverted to Initial")
                    }
                }
            }

        log { "Upload completion detected" }
    }

    private suspend fun updateMetadata(
        endpoint: UploadEndpoint,
        caption: String,
        hashtags: List<String>,
    ) {
        log { "Updating metadata" }
        _state.update { it.copy(updateMetadataUiState = UiState.InProgress(0f)) }

        val hashtagsList =
            hashtags
                .map { it.trim() }
                .filter { it.isNotBlank() }

        val uploadFileRequest =
            UploadFileRequest(
                videoUid = endpoint.videoID,
                caption = caption,
                hashtags = hashtagsList,
            )

        try {
            coroutineScope {
                // Launch fake progress animation
                val progressJob =
                    launch {
                        fakeProgressAnimation()
                    }

                // Perform the actual metadata update
                updateMeta(UpdateMetaUseCase.Param(uploadFileRequest))
                log { "Metadata updated" }

                // Cancel the progress animation and set to 100%
                progressJob.cancel()
                _state.update { it.copy(updateMetadataUiState = UiState.InProgress(1.0f)) }

                // Small delay to show 100% progress before transitioning to success
                delay(SUCCESS_TRANSITION_DELAY_MS)

                _state.update { it.copy(updateMetadataUiState = UiState.Success(Unit)) }
                send(Event.UploadSuccess)
                uploadVideoTelemetry.uploadSuccess(endpoint.videoID)
                performPostPublishCleanup()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            log { "Error updating metadata" }
            _state.update { it.copy(updateMetadataUiState = UiState.Failure(e)) }
            send(Event.UploadFailed(e))
        }
    }

    private suspend fun fakeProgressAnimation() {
        val durationMs = FAKE_PROGRESS_ANIMATION_DURATION_MS // 2 seconds total animation
        val steps = FAKE_PROGRESS_STEPS
        val stepDuration = durationMs / steps
        val progressIncrement = 1.0f / steps

        repeat(steps) { step ->
            val currentProgress = progressIncrement * step
            _state.update { it.copy(updateMetadataUiState = UiState.InProgress(currentProgress)) }
            delay(stepDuration)
        }
    }

    private fun performPostPublishCleanup() {
        cleanup()
    }

    private fun resetState() {
        cleanup()
        resetUploadStates()
        _state.value = ViewState()
    }

    private fun resetUploadStates() {
        _state.update {
            it.copy(
                fileUploadUiState = UiState.Initial,
                updateMetadataUiState = UiState.Initial,
            )
        }
    }

    private fun cleanup() {
        cancelUpload()
        cancelCompleteProcess()
        deleteSelectedFile()
    }

    private fun cancelUpload() {
        backgroundUploadJob?.cancel()
        backgroundUploadJob = null
    }

    private fun cancelCompleteProcess() {
        completeProcessJob?.cancel()
        completeProcessJob = null
    }

    private fun deleteSelectedFile() {
        state.value.selectedFilePath?.let {
            viewModelScope.launch {
                runCatching {
                    withContext(appDispatchers.disk) {
                        SystemFileSystem.delete(Path(it), mustExist = false)
                    }
                }.onFailure { error -> crashlyticsManager.recordException(YralException(error)) }
            }
        }
    }

    fun pushScreenView() {
        uploadVideoTelemetry.uploadVideoScreenViewed()
    }

    fun pushSelectFile() {
        uploadVideoTelemetry.selectFile()
    }

    fun pushUploadFailed(e: Throwable) {
        if (!_state.value.errorAnalyticsPushed) {
            uploadVideoTelemetry.uploadFailed(e.message ?: "")
            _state.update { it.copy(errorAnalyticsPushed = true) }
        }
    }

    private inline fun log(message: () -> String) {
        @Suppress("KotlinConstantConditions")
        if (!LOG_ENABLED) return
        logger.v(message = message)
    }

    sealed class Event {
        sealed class ShowInputError : Event() {
            data object SelectFile : ShowInputError()
            data object AddCaption : ShowInputError()
            data object AddHashtags : ShowInputError()
        }

        data object UploadSuccess : Event()
        data class UploadFailed(
            val error: Throwable,
        ) : Event()

        data object GoToHome : Event()
    }

    data class ViewState(
        val selectedFilePath: String? = null,
        val caption: String = "",
        val hashtags: List<String> = emptyList(),
        val errorAnalyticsPushed: Boolean = false,
        val fileUploadUiState: UiState<UploadEndpoint> = UiState.Initial,
        val updateMetadataUiState: UiState<Unit> = UiState.Initial,
    ) {
        val canUpload: Boolean =
            !selectedFilePath.isNullOrBlank() &&
                caption.isNotBlank() &&
                hashtags.isNotEmpty() &&
                updateMetadataUiState !is UiState.InProgress

        // Unified upload state that shows the complete flow progress to the user
        // updateMetadataUiState is given priority over fileUploadUiState as uploadUiState should be
        // shown only when user clicks on upload button
        val uploadUiState: UiState<Any>
            get() =
                when (updateMetadataUiState) {
                    is UiState.InProgress ->
                        when (fileUploadUiState) {
                            is UiState.InProgress -> fileUploadUiState.normalize(PROGRESS_WEIGHT_UPLOAD)
                            is UiState.Failure -> fileUploadUiState
                            UiState.Initial -> updateMetadataUiState
                            is UiState.Success<*> ->
                                updateMetadataUiState.normalize(
                                    PROGRESS_WEIGHT_METADATA,
                                    PROGRESS_WEIGHT_UPLOAD,
                                )
                        }

                    else -> updateMetadataUiState
                }

        private fun UiState.InProgress.normalize(
            weight: Float,
            previousProgress: Float = 0f,
        ): UiState.InProgress {
            val normalizedProgress = previousProgress + (progress * weight)
            return UiState.InProgress(normalizedProgress)
        }

        private companion object {
            const val PROGRESS_WEIGHT_UPLOAD = 0.9f
            const val PROGRESS_WEIGHT_METADATA = 0.1f
        }
    }

    companion object {
        private const val LOG_ENABLED = false
        private const val FAKE_PROGRESS_ANIMATION_DURATION_MS = 2000L // 2 seconds total animation
        private const val FAKE_PROGRESS_STEPS = 40
        private const val SUCCESS_TRANSITION_DELAY_MS = 200L // 200 ms delay after success
    }
}
