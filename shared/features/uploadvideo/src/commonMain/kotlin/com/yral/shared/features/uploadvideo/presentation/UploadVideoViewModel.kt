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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private fun send(event: Event) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    fun onFileSelected(filePath: String) {
        _state.update { it.copy(selectedFilePath = filePath) }
    }

    fun onCaptionChanged(caption: String) {
        _state.update { it.copy(caption = caption) }
    }

    fun onHashtagsChanged(hashtags: List<String>) {
        _state.update { it.copy(hashtags = hashtags) }
    }

    fun onUploadButtonClicked() {
        validateAndUpload()
    }

    fun onUploadDoneClicked() {
        if (state.value.uploadUiState is UiState.Success) {
            resetState()
        }
    }

    fun onRetryClicked() {
        validateAndUpload()
    }

    fun onGoToHomeClicked() {
        resetState()
        send(Event.GoToHome)
    }

    @Suppress("ReturnCount")
    private fun validateAndUpload() {
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

        if (currentState.uploadUiState is UiState.InProgress) {
            return
        }

        startCompleteUploadProcess(
            currentState.selectedFilePath,
            currentState.caption,
            currentState.hashtags,
        )
    }

    private fun resetState() {
        deleteSelectedFile()
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

    private fun startCompleteUploadProcess(
        filePath: String,
        caption: String,
        hashtags: List<String>,
    ) {
        _state.update {
            it.copy(uploadUiState = UiState.InProgress(0f))
        }

        viewModelScope.launch {
            try {
                // Step 1: Get upload endpoint
                val endpointResult = getUploadEndpoint()
                val endpoint = endpointResult.getOrThrow()

                // Step 2: Start video upload
                uploadVideo(UploadVideoUseCase.Params(endpoint.url, filePath))
                    .collect { result ->
                        result.fold(
                            success = { uploadState ->
                                when (uploadState) {
                                    is UploadState.Uploaded -> {
                                        // Step 3: Update metadata
                                        updateMetadata(endpoint, caption, hashtags)
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
                                                uploadUiState =
                                                    UiState.InProgress(
                                                        progress,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            },
                            failure = { error ->
                                _state.update { it.copy(uploadUiState = UiState.Failure(error)) }
                                send(Event.UploadFailed(error))
                            },
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                _state.update { it.copy(uploadUiState = UiState.Failure(e)) }
                send(Event.UploadFailed(e))
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

            _state.update { it.copy(uploadUiState = UiState.Success(Unit)) }
            send(Event.UploadSuccess)
            deleteSelectedFile()
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            _state.update { it.copy(uploadUiState = UiState.Failure(e)) }
            send(Event.UploadFailed(e))
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
        val uploadUiState: UiState<Unit> = UiState.Initial,
    ) {
        val canUpload: Boolean =
            !selectedFilePath.isNullOrBlank() &&
                caption.isNotBlank() &&
                !hashtags.isEmpty() &&
                uploadUiState !is UiState.InProgress
    }
}
