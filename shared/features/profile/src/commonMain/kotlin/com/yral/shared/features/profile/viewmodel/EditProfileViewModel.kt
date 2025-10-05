package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import com.yral.shared.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditProfileViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileViewState())
    val state: StateFlow<EditProfileViewState> = _state.asStateFlow()

    init {
        val profilePic = sessionManager.profilePic
        val uniqueId = sessionManager.userPrincipal.orEmpty()
        _state.update {
            it.copy(
                profileImageUrl = profilePic,
                usernameInput = "",
                uniqueId = uniqueId,
            )
        }
    }

    fun onUsernameChanged(value: String) {
        val sanitized = value.trim().removePrefix("@")
        _state.update { it.copy(usernameInput = sanitized) }
    }

    fun onUsernameFocusChanged(isFocused: Boolean) {
        _state.update { it.copy(isUsernameFocused = isFocused) }
    }
}

data class EditProfileViewState(
    val profileImageUrl: String? = null,
    val usernameInput: String = "",
    val uniqueId: String = "",
    val isUsernameFocused: Boolean = false,
)
