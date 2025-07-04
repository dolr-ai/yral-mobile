package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.auth.utils.getAccountInfo
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.rust.domain.models.FeedDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    sessionManager: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    init {
        _state.update { it.copy(accountInfo = sessionManager.getAccountInfo()) }
        loadVideos()
    }

    fun confirmDelete(videoID: String?) {
        _state.update { it.copy(deleteConfirmation = videoID) }
    }

    fun deleteVideo() {
        updateDeleting(_state.value.deleteConfirmation)
    }

    private fun updateDeleting(videoID: String?) {
        _state.update { currentState ->
            val updatedVideos =
                (currentState.uiState as? UiState.Success)?.data?.videos?.map { video ->
                    if (video.feedDetail.videoID == videoID) {
                        video.copy(isDeleting = true)
                    } else {
                        video
                    }
                }

            val updatedUiState =
                when (val ui = currentState.uiState) {
                    is UiState.Success ->
                        ui.copy(
                            data = ui.data.copy(videos = updatedVideos ?: ui.data.videos),
                        )
                    else -> ui
                }

            currentState.copy(
                deleteConfirmation = null,
                openedVideo = currentState.openedVideo?.copy(isDeleting = true),
                uiState = updatedUiState,
            )
        }
    }

    fun openVideo(video: ProfileVideo?) {
        _state.update { it.copy(openedVideo = video) }
    }

    fun loadVideos() {
        _state.update {
            it.copy(
                isRefreshing = false,
                uiState =
                    UiState.Success(
                        ProfileVideos(
                            videos = listOf(),
                            hasMorePages = false,
                        ),
                    ),
            )
        }
    }

    fun onRefresh() {
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            loadVideos()
        }
    }
}

data class ViewState(
    val accountInfo: AccountInfo? = null,
    val deleteConfirmation: String? = null,
    val openedVideo: ProfileVideo? = null,
    val isRefreshing: Boolean = false,
    val uiState: UiState<ProfileVideos> = UiState.Initial,
)

data class ProfileVideos(
    val videos: List<ProfileVideo>,
    val hasMorePages: Boolean,
)

data class ProfileVideo(
    val feedDetail: FeedDetails,
    var isDeleting: Boolean = false,
)
