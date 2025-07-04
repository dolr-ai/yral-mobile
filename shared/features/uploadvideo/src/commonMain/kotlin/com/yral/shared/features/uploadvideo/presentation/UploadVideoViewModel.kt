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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class UploadVideoViewModel(
    private val getUploadEndpoint: GetUploadEndpointUseCase,
    private val uploadVideo: UploadVideoUseCase,
    private val updateMeta: UpdateMetaUseCase,
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

    fun onHashtagsChanged(hashtags: String) {
        _state.update { it.copy(hashtags = hashtags) }
    }

    @Suppress("ReturnCount")
    fun onUploadButtonClicked() {
        val currentState = _state.value

        // Validate inputs
        if (currentState.selectedFilePath == null) {
            send(Event.ShowError.SelectFile)
            return
        }

        if (currentState.caption.isNullOrBlank()) {
            send(Event.ShowError.AddCaption)
            return
        }

        if (currentState.hashtags.isNullOrBlank()) {
            send(Event.ShowError.AddHashtags)
            return
        }

        startCompleteUploadProcess(
            currentState.selectedFilePath,
            currentState.caption,
            currentState.hashtags,
        )
    }

    private fun startCompleteUploadProcess(
        filePath: String,
        caption: String,
        hashtags: String,
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
                                send(Event.ShowError.UploadFailed(error))
                            },
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                _state.update { it.copy(uploadUiState = UiState.Failure(e)) }
                send(Event.ShowError.UploadFailed(e))
            }
        }
    }

    private suspend fun updateMetadata(
        endpoint: UploadEndpoint,
        caption: String,
        hashtags: String,
    ) {
        try {
            val hashtagsList =
                hashtags
                    .split(",")
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
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            _state.update { it.copy(uploadUiState = UiState.Failure(e)) }
            send(Event.ShowError.UploadFailed(e))
        }
    }

    internal sealed class Event {
        internal sealed class ShowError : Event() {
            internal data object SelectFile : ShowError()
            internal data object AddCaption : ShowError()
            internal data object AddHashtags : ShowError()
            internal data class UploadFailed(
                val error: Throwable,
            ) : ShowError()
        }

        internal data object UploadSuccess : Event()
    }

    internal data class ViewState(
        val selectedFilePath: String? = null,
        val caption: String? = null,
        val hashtags: String? = null,
        val uploadUiState: UiState<Unit> = UiState.Initial,
    ) {
        val canUpload: Boolean =
            selectedFilePath != null &&
                !caption.isNullOrBlank() &&
                !hashtags.isNullOrBlank() &&
                (uploadUiState is UiState.Initial || uploadUiState is UiState.Failure)
    }
}
