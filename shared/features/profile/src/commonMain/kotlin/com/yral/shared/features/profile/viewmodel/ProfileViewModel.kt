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
                    pagingData.filter { video -> video.videoID !in deletedIds }
                }.distinctUntilChanged()
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
        updateDeleteConfirmationIfDifferent(newDeleteState)
    }

    fun deleteVideo() {
        val currentState = _state.value
        val deleteRequest =
            when (val deleteState = currentState.deleteConfirmation) {
                is DeleteConfirmationState.AwaitingConfirmation -> deleteState.request
                is DeleteConfirmationState.Error -> deleteState.request
                else -> return
            }
        viewModelScope.launch {
            _state.update { state ->
                state.copy(deleteConfirmation = DeleteConfirmationState.InProgress(deleteRequest))
            }
            deleteVideoUseCase
                .invoke(deleteRequest)
                .onSuccess {
                    deletedVideoIds.update { currentDeletedIds ->
                        currentDeletedIds + deleteRequest.videoId
                    }
                    _state.update { currentState ->
                        val reelState = currentState.videoView as? VideoViewState.ViewingReels
                        if (reelState != null) {
                            currentState.copy(
                                deleteConfirmation = DeleteConfirmationState.None,
                                videoView = reelState.copy(deletedVideoId = deleteRequest.videoId),
                            )
                        } else {
                            currentState.copy(deleteConfirmation = DeleteConfirmationState.None)
                        }
                    }
                }.onFailure { error ->
                    _state.update { state ->
                        state.copy(deleteConfirmation = DeleteConfirmationState.Error(deleteRequest, error))
                    }
                }
        }
    }

    fun dismissDeleteError() {
        updateDeleteConfirmationIfDifferent(DeleteConfirmationState.None)
    }

    fun openVideoReel(clickedIndex: Int) {
        val newVideoState =
            VideoViewState.ViewingReels(
                currentIndex = clickedIndex,
                deletedVideoId = "",
            )
        updateVideoViewIfDifferent(newVideoState)
    }

    fun closeVideoReel() {
        updateVideoViewIfDifferent(VideoViewState.None)
    }

    fun onReelPageChanged(newIndex: Int) {
        val currentState = _state.value
        val reelState = currentState.videoView as? VideoViewState.ViewingReels ?: return
        if (reelState.currentIndex != newIndex) {
            updateVideoViewIfDifferent(reelState.copy(currentIndex = newIndex))
        }
    }

    fun clearDeletedVideoId() {
        val currentState = _state.value
        val reelState = currentState.videoView as? VideoViewState.ViewingReels ?: return
        if (reelState.deletedVideoId.isNotEmpty()) {
            updateVideoViewIfDifferent(reelState.copy(deletedVideoId = ""))
        }
    }

    fun handleReelVideoDeleted(
        newIndex: Int?,
        shouldClose: Boolean,
    ) {
        if (shouldClose) {
            closeVideoReel()
        } else if (newIndex != null) {
            val currentState = _state.value
            val reelState = currentState.videoView as? VideoViewState.ViewingReels ?: return
            updateVideoViewIfDifferent(
                reelState.copy(
                    currentIndex = newIndex,
                    deletedVideoId = "",
                ),
            )
        }
    }

    private fun updateDeleteConfirmationIfDifferent(newState: DeleteConfirmationState) {
        _state.update { currentState ->
            if (currentState.deleteConfirmation != newState) {
                currentState.copy(deleteConfirmation = newState)
            } else {
                currentState
            }
        }
    }
    private fun updateVideoViewIfDifferent(newState: VideoViewState) {
        _state.update { currentState ->
            if (currentState.videoView != newState) {
                currentState.copy(videoView = newState)
            } else {
                currentState
            }
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
    data class ViewingReels(
        val currentIndex: Int = 0,
        val deletedVideoId: String = "",
    ) : VideoViewState()
}
