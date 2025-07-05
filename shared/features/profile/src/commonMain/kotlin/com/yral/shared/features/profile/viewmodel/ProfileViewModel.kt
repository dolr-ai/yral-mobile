package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.auth.utils.getAccountInfo
import com.yral.shared.features.profile.data.ProfileVideosPagingSource
import com.yral.shared.features.profile.domain.DeleteVideoUseCase
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.rust.domain.models.FeedDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val sessionManager: SessionManager,
    private val profileRepository: ProfileRepository,
    private val deleteVideoUseCase: DeleteVideoUseCase,
) : ViewModel() {
    companion object {
        private const val POSTS_PER_PAGE = 20
        private const val POSTS_PREFETCH_DISTANCE = 5
    }

    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    // Track locally deleted videos to filter them out
    private val deletedVideoIds = MutableStateFlow<Set<String>>(emptySet())

    private val basePagingData: Flow<PagingData<FeedDetails>>

    val profileVideos: Flow<PagingData<FeedDetails>>

    init {
        _state.update { it.copy(accountInfo = sessionManager.getAccountInfo()) }

        basePagingData =
            Pager(
                config =
                    PagingConfig(
                        pageSize = POSTS_PER_PAGE,
                        prefetchDistance = POSTS_PREFETCH_DISTANCE,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = {
                    ProfileVideosPagingSource(
                        profileRepository = profileRepository,
                        sessionManager = sessionManager,
                    )
                },
            ).flow.cachedIn(viewModelScope)

        profileVideos =
            basePagingData
                .combine(deletedVideoIds) { pagingData, deletedIds ->
                    pagingData.filter { video ->
                        video.videoID !in deletedIds
                    }
                }.distinctUntilChanged() // Keep this - PagingData filtering might produce same results
    }

    fun confirmDelete(
        videoId: String?,
        postId: ULong?,
    ) {
        val newDeleteState =
            when {
                videoId != null && postId != null -> {
                    DeleteConfirmationState.AwaitingConfirmation(
                        request =
                            DeleteVideoRequest(
                                postId = postId,
                                videoId = videoId,
                            ),
                    )
                }

                else -> DeleteConfirmationState.None
            }

        if (_state.value.deleteConfirmation != newDeleteState) {
            _state.update { it.copy(deleteConfirmation = newDeleteState) }
        }
    }

    fun deleteVideo() {
        val currentState = _state.value
        val deleteRequest =
            when (val deleteState = currentState.deleteConfirmation) {
                is DeleteConfirmationState.AwaitingConfirmation -> deleteState.request
                is DeleteConfirmationState.Error -> deleteState.request
                else -> return // No delete in progress
            }
        viewModelScope.launch {
            // Only update if not already in progress
            val newInProgressState = DeleteConfirmationState.InProgress(deleteRequest)
            if (currentState.deleteConfirmation != newInProgressState) {
                _state.update { state ->
                    state.copy(deleteConfirmation = newInProgressState)
                }
            }

            deleteVideoUseCase
                .invoke(deleteRequest)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            videoView = VideoViewState.None,
                            deleteConfirmation = DeleteConfirmationState.None,
                        )
                    }
                    // Remove video locally by adding it to deleted set
                    deletedVideoIds.update { currentDeletedIds ->
                        currentDeletedIds + deleteRequest.videoId
                    }
                }.onFailure { error ->
                    _state.update { state ->
                        state.copy(
                            deleteConfirmation =
                                DeleteConfirmationState.Error(
                                    deleteRequest,
                                    error,
                                ),
                        )
                    }
                }
        }
    }

    fun dismissDeleteError() {
        val currentState = _state.value
        if (currentState.deleteConfirmation != DeleteConfirmationState.None) {
            _state.update { state ->
                state.copy(deleteConfirmation = DeleteConfirmationState.None)
            }
        }
    }

    fun openVideo(video: FeedDetails?) {
        val currentState = _state.value
        val newVideoState =
            if (video != null) {
                VideoViewState.Viewing(video)
            } else {
                VideoViewState.None
            }

        if (currentState.videoView != newVideoState) {
            _state.update { it.copy(videoView = newVideoState) }
        }
    }
}

data class ViewState(
    val accountInfo: AccountInfo? = null,
    val deleteConfirmation: DeleteConfirmationState = DeleteConfirmationState.None,
    val videoView: VideoViewState = VideoViewState.None,
)

sealed class DeleteConfirmationState {
    data object None : DeleteConfirmationState()
    data class AwaitingConfirmation(
        val request: DeleteVideoRequest,
    ) : DeleteConfirmationState()

    data class InProgress(
        val request: DeleteVideoRequest,
    ) : DeleteConfirmationState()

    data class Error(
        val request: DeleteVideoRequest,
        val error: Throwable,
    ) : DeleteConfirmationState()
}

sealed class VideoViewState {
    data object None : VideoViewState()
    data class Viewing(
        val video: FeedDetails,
    ) : VideoViewState()
}
