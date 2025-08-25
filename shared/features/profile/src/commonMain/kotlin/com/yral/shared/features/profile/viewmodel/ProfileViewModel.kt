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
import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.auth.utils.getAccountInfo
import com.yral.shared.features.profile.analytics.ProfileTelemetry
import com.yral.shared.features.profile.domain.DeleteVideoUseCase
import com.yral.shared.features.profile.domain.ProfileVideosPagingSource
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
    private val profileTelemetry: ProfileTelemetry,
) : ViewModel() {
    companion object {
        private const val POSTS_PER_PAGE = 20
        private const val POSTS_PREFETCH_DISTANCE = 5
    }

    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private val deletedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val profileVideos: Flow<PagingData<FeedDetails>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = POSTS_PER_PAGE,
                    initialLoadSize = POSTS_PER_PAGE,
                    prefetchDistance = POSTS_PREFETCH_DISTANCE,
                    enablePlaceholders = false,
                ),
            pagingSourceFactory = {
                ProfileVideosPagingSource(
                    profileRepository = profileRepository,
                    sessionManager = sessionManager,
                )
            },
        ).flow
            .cachedIn(viewModelScope)
            .combine(deletedVideoIds) { pagingData, deletedIds ->
                val videoIds = mutableSetOf<String>()
                pagingData
                    .filter { video ->
                        video.videoID.isNotEmpty() &&
                            videoIds.add(video.videoID) &&
                            video.videoID !in deletedIds
                    }
            }.distinctUntilChanged()

    init {
        _state.update { it.copy(accountInfo = sessionManager.getAccountInfo()) }
    }

    fun confirmDelete(
        feedDetails: FeedDetails,
        ctaType: VideoDeleteCTA,
    ) {
        if (_state.value.deleteConfirmation !is DeleteConfirmationState.None) return
        profileTelemetry.onVideoClicked(feedDetails)
        updateDeleteConfirmationIfDifferent(
            DeleteConfirmationState.AwaitingConfirmation(
                request =
                    DeleteVideoRequest(
                        feedDetails = feedDetails,
                        ctaType = ctaType,
                    ),
            ),
        )
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
            profileTelemetry.onDeleteInitiated(
                feedDetails = deleteRequest.feedDetails,
            )
            _state.update { state ->
                state.copy(deleteConfirmation = DeleteConfirmationState.InProgress(deleteRequest))
            }
            deleteVideoUseCase
                .invoke(deleteRequest)
                .onSuccess {
                    profileTelemetry.onDeleted(
                        feedDetails = deleteRequest.feedDetails,
                        catType = deleteRequest.ctaType,
                    )
                    deletedVideoIds.update { it + deleteRequest.feedDetails.videoID }

                    // Update session manager with new video count
                    val currentCount = sessionManager.profileVideosCount()
                    sessionManager.updateProfileVideosCount(
                        count = (currentCount - 1).coerceAtLeast(0),
                    )
                    _state.update { it.copy(deleteConfirmation = DeleteConfirmationState.None) }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            deleteConfirmation = DeleteConfirmationState.Error(deleteRequest, error),
                        )
                    }
                }
        }
    }

    fun clearDeleteConfirmationState() {
        updateDeleteConfirmationIfDifferent(DeleteConfirmationState.None)
    }

    fun openVideoReel(clickedIndex: Int) {
        updateVideoViewIfDifferent(VideoViewState.ViewingReels(clickedIndex))
    }

    fun closeVideoReel() {
        updateVideoViewIfDifferent(VideoViewState.None)
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

    fun pushScreenView(totalVideos: Int) {
        profileTelemetry.onProfileScreenViewed(
            totalVideos = totalVideos,
            publisherUserId = state.value.accountInfo?.userPrincipal ?: "",
        )
    }

    fun uploadVideoClicked() {
        profileTelemetry.onUploadVideoClicked()
    }

    fun setManualRefreshTriggered(isTriggered: Boolean) {
        _state.update { it.copy(manualRefreshTriggered = isTriggered) }
        if (isTriggered) {
            sessionManager.updateProfileVideosCount(null)
        }
    }
}

data class ViewState(
    val accountInfo: AccountInfo? = null,
    val deleteConfirmation: DeleteConfirmationState = DeleteConfirmationState.None,
    val videoView: VideoViewState = VideoViewState.None,
    val manualRefreshTriggered: Boolean = false,
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
        val initialPage: Int = 0,
    ) : VideoViewState()
}
