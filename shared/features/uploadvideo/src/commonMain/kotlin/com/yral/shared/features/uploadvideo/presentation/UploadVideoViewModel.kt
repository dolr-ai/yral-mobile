package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.getOrThrow
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class UploadVideoViewModel internal constructor(
    private val getUploadEndpoint: GetUploadEndpointUseCase,
    private val uploadVideo: UploadVideoUseCase,
    private val updateMeta: UpdateMetaUseCase,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = eventChannel.receiveAsFlow()

    private var backgroundUploadJob: Job? = null
    private var currentUploadEndpoint: UploadEndpoint? = null

    private fun send(event: Event) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    fun onFileSelected(filePath: String) {
        _state.update { it.copy(selectedFilePath = filePath) }
        startBackgroundUpload(filePath)
    }

    fun onCaptionChanged(caption: String) {
        _state.update { it.copy(caption = caption) }
    }

    fun onHashtagsChanged(hashtags: List<String>) {
        _state.update { it.copy(hashtags = hashtags) }
    }

    fun onUploadButtonClicked() {
        validateAndPublish()
    }

    fun onUploadDoneClicked() {
        if (state.value.completeProcessUiState is UiState.Success) {
            resetState()
        }
    }

    fun onRetryClicked() {
        validateAndPublish()
    }

    fun onGoToHomeClicked() {
        resetState()
        send(Event.GoToHome)
    }

    private fun startBackgroundUpload(filePath: String) {
        // Cancel any existing upload
        backgroundUploadJob?.cancel()
        currentUploadEndpoint = null
        
        _state.update {
            it.copy(
                fileUploadUiState = UiState.InProgress(0f),
                completeProcessUiState = UiState.Initial
            )
        }

        backgroundUploadJob = viewModelScope.launch {
            performBackgroundUpload(filePath)
        }
    }

    private suspend fun performBackgroundUpload(filePath: String) {
        try {
            // Ensure we have upload endpoint
            ensureUploadEndpoint()
            val endpoint = currentUploadEndpoint!!

            // Start video upload
            uploadVideo(UploadVideoUseCase.Params(endpoint.url, filePath))
                .collect { result ->
                    result.fold(
                        success = { uploadState ->
                            when (uploadState) {
                                is UploadState.Uploaded -> {
                                    _state.update {
                                        it.copy(fileUploadUiState = UiState.Success(Unit))
                                    }
                                }

                                is UploadState.InProgress -> {
                                    val progress =
                                        if (uploadState.totalBytes != null) {
                                            uploadState.bytesSent.toFloat() / uploadState.totalBytes.toFloat()
                                        } else {
                                            0f
                                        }
                                    _state.update {
                                        it.copy(
                                            fileUploadUiState = UiState.InProgress(progress)
                                        )
                                    }
                                }
                            }
                        },
                        failure = { error ->
                            _state.update { it.copy(fileUploadUiState = UiState.Failure(error)) }
                        }
                    )
                }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            _state.update { it.copy(fileUploadUiState = UiState.Failure(e)) }
        }
    }



    @Suppress("ReturnCount")
    private fun validateAndPublish() {
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

        if (currentState.completeProcessUiState is UiState.InProgress) {
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
        _state.update {
            it.copy(completeProcessUiState = UiState.InProgress(0f))
        }

        viewModelScope.launch {
            try {
                when (val fileUploadState = state.value.fileUploadUiState) {
                    is UiState.Failure -> {
                        // File upload failed, retry upload first
                        performBackgroundUpload(filePath)
                    }
                    
                    UiState.Initial -> {
                        // No upload started, start fresh
                        ensureUploadEndpoint()
                        performBackgroundUpload(filePath)
                    }
                    
                    is UiState.Success, is UiState.InProgress -> {
                        // File upload completed or in progress, proceed
                    }
                }
                
                // Wait for upload completion and update metadata
                waitForUploadCompletionAndUpdateMetadata(caption, hashtags)
                
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _state.update { it.copy(completeProcessUiState = UiState.Failure(e)) }
                send(Event.UploadFailed(e))
            }
        }
    }

    private suspend fun ensureUploadEndpoint() {
        if (currentUploadEndpoint == null) {
            val endpointResult = getUploadEndpoint()
            currentUploadEndpoint = endpointResult.getOrThrow()
        }
    }

    private suspend fun waitForUploadCompletionAndUpdateMetadata(
        caption: String,
        hashtags: List<String>
    ) {
        // Monitor file upload state until completion using Flow collection
        state.map { it.fileUploadUiState }
            .distinctUntilChanged()
            .collect { fileUploadState ->
                when (fileUploadState) {
                    is UiState.Success -> {
                        currentUploadEndpoint?.let { endpoint ->
                            updateMetadata(endpoint, caption, hashtags)
                        } ?: run {
                            throw IllegalStateException("Upload endpoint not available")
                        }
                        return@collect
                    }
                    
                    is UiState.Failure -> {
                        throw fileUploadState.error
                    }
                    
                    is UiState.InProgress, UiState.Initial -> {
                        // Continue waiting for completion
                    }
                }
            }
    }

    private suspend fun updateMetadata(
        endpoint: UploadEndpoint,
        caption: String,
        hashtags: List<String>,
    ) {
        try {
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

            updateMeta(UpdateMetaUseCase.Param(uploadFileRequest))

            _state.update { it.copy(completeProcessUiState = UiState.Success(Unit)) }
            send(Event.UploadSuccess)
            deleteSelectedFile()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            _state.update { it.copy(completeProcessUiState = UiState.Failure(e)) }
            send(Event.UploadFailed(e))
        }
    }

    private fun resetState() {
        backgroundUploadJob?.cancel()
        deleteSelectedFile()
        currentUploadEndpoint = null
        _state.value = ViewState()
    }

    private fun deleteSelectedFile() {
        state.value.selectedFilePath?.let {
            viewModelScope.launch {
                withContext(appDispatchers.disk) {
                    SystemFileSystem.delete(Path(it), mustExist = false)
                }
            }
        }
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
        val fileUploadUiState: UiState<Unit> = UiState.Initial,
        val completeProcessUiState: UiState<Unit> = UiState.Initial,
    ) {
        val canUpload: Boolean =
            !selectedFilePath.isNullOrBlank() &&
                caption.isNotBlank() &&
                hashtags.isNotEmpty() &&
                completeProcessUiState !is UiState.InProgress

        // Legacy property for backward compatibility
        val uploadUiState: UiState<Unit> get() = completeProcessUiState
    }


}
