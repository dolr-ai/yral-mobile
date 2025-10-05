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
    companion object {
        private const val MIN_USERNAME_LENGTH = 3
    }

    private val _state = MutableStateFlow(EditProfileViewState())
    val state: StateFlow<EditProfileViewState> = _state.asStateFlow()

    init {
        val profilePic = sessionManager.profilePic
        val uniqueId = sessionManager.userPrincipal.orEmpty()
        val initialUsername = sanitizeInput("")
        _state.update {
            it.copy(
                profileImageUrl = profilePic,
                usernameInput = initialUsername,
                initialUsername = initialUsername,
                uniqueId = uniqueId,
                isUsernameValid = true,
            )
        }
    }

    fun onUsernameChanged(value: String) {
        val sanitized = sanitizeInput(value)
        _state.update {
            it.copy(
                usernameInput = sanitized,
                isUsernameValid = isValidOrEmpty(sanitized),
            )
        }
    }

    fun onUsernameFocusChanged(isFocused: Boolean) {
        _state.update {
            val currentInitial =
                if (isFocused && !it.isUsernameFocused) {
                    it.usernameInput
                } else {
                    it.initialUsername
                }
            val sanitized = sanitizeInput(it.usernameInput)
            if (!isFocused && !isValidOrEmpty(sanitized)) {
                it.copy(
                    isUsernameFocused = false,
                    usernameInput = it.initialUsername,
                    isUsernameValid = true,
                )
            } else {
                it.copy(
                    isUsernameFocused = isFocused,
                    initialUsername = currentInitial,
                    isUsernameValid = if (isFocused) isValidOrEmpty(sanitized) else it.isUsernameValid,
                )
            }
        }
    }

    fun validateCurrentUsername(): Boolean {
        val sanitized = sanitizeInput(_state.value.usernameInput)
        val isValid = isValidUsername(sanitized)
        _state.update {
            it.copy(
                usernameInput = sanitized,
                isUsernameValid = isValid,
            )
        }
        return isValid
    }

    fun applyUsernameChange() {
        _state.update {
            val sanitized = sanitizeInput(it.usernameInput)
            it.copy(
                usernameInput = sanitized,
                initialUsername = sanitized,
                isUsernameFocused = false,
                isUsernameValid = true,
            )
        }
    }

    fun revertUsernameChange() {
        _state.update {
            val shouldClear = sanitizeInput(it.usernameInput).isEmpty()
            it.copy(
                usernameInput = if (shouldClear) "" else it.initialUsername,
                isUsernameFocused = false,
                isUsernameValid = true,
            )
        }
    }

    fun sanitizedUsername(): String = sanitizeInput(_state.value.usernameInput)

    private fun sanitizeInput(value: String): String = value.trim().removePrefix("@")

    private fun isValidUsername(value: String): Boolean =
        value.length > MIN_USERNAME_LENGTH &&
            value.all { it.isLetterOrDigit() }

    private fun isValidOrEmpty(value: String): Boolean = value.isEmpty() || isValidUsername(value)
}

data class EditProfileViewState(
    val profileImageUrl: String? = null,
    val usernameInput: String = "",
    val uniqueId: String = "",
    val initialUsername: String = "",
    val isUsernameFocused: Boolean = false,
    val isUsernameValid: Boolean = true,
)
