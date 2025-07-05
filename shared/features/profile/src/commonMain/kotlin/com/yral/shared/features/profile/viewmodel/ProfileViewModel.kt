package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.auth.utils.getAccountInfo
import com.yral.shared.features.profile.data.ProfileVideosPagingSource
import com.yral.shared.features.profile.domain.GetProfileVideosUseCase
import com.yral.shared.rust.domain.models.FeedDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProfileViewModel(
    private val sessionManager: SessionManager,
    private val getProfileVideosUseCase: GetProfileVideosUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    val profileVideos: Flow<PagingData<ProfileVideo>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = 20,
                    prefetchDistance = 5,
                    enablePlaceholders = false,
                ),
            pagingSourceFactory = {
                ProfileVideosPagingSource(
                    getProfileVideosUseCase = getProfileVideosUseCase,
                    sessionManager = sessionManager,
                )
            },
        ).flow.cachedIn(viewModelScope)

    init {
        _state.update { it.copy(accountInfo = sessionManager.getAccountInfo()) }
    }

    fun confirmDelete(videoID: String?) {
        _state.update { it.copy(deleteConfirmation = videoID) }
    }

    fun deleteVideo() {
        updateDeleting(_state.value.deleteConfirmation)
    }

    private fun updateDeleting(videoID: String?) {
        _state.update { currentState ->
            currentState.copy(
                deleteConfirmation = null,
                openedVideo =
                    currentState.openedVideo?.let { video ->
                        if (video.feedDetail.videoID == videoID) {
                            video.copy(isDeleting = true)
                        } else {
                            video
                        }
                    },
            )
        }
    }

    fun openVideo(video: ProfileVideo?) {
        _state.update { it.copy(openedVideo = video) }
    }
}

data class ViewState(
    val accountInfo: AccountInfo? = null,
    val deleteConfirmation: String? = null,
    val openedVideo: ProfileVideo? = null,
)

data class ProfileVideo(
    val feedDetail: FeedDetails,
    var isDeleting: Boolean = false,
)
