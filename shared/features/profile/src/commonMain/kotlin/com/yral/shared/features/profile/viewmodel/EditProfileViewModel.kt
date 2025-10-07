package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.rust.service.services.HelperService
import com.yral.shared.rust.service.services.MetadataUpdateError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        println("Sarvesh: ${sessionManager.username}")
        val initialUsername = sanitizeInput(sessionManager.username.orEmpty())
        _state.update {
            it.copy(
                profileImageUrl = profilePic,
                usernameInput = initialUsername,
                initialUsername = initialUsername,
                uniqueId = uniqueId,
                isUsernameValid = true,
                usernameErrorMessage = null,
            )
        }
    }

    fun onUsernameChanged(value: String) {
        val sanitized = sanitizeInput(value)
        _state.update {
            it.copy(
                usernameInput = sanitized,
                isUsernameValid = isValidOrEmpty(sanitized),
                usernameErrorMessage = null,
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
                    usernameErrorMessage = null,
                )
            } else {
                it.copy(
                    isUsernameFocused = isFocused,
                    initialUsername = currentInitial,
                    isUsernameValid =
                        if (isFocused && it.usernameErrorMessage == null) {
                            isValidOrEmpty(sanitized)
                        } else {
                            it.isUsernameValid
                        },
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
                usernameErrorMessage = if (isValid) null else it.usernameErrorMessage,
            )
        }
        return isValid
    }

    fun applyUsernameChange() {
        val sanitized = sanitizeInput(_state.value.usernameInput)
        val identity = sessionManager.identity ?: return
        val userCanisterId = sessionManager.canisterID ?: return
        val previousUsername = _state.value.initialUsername

        viewModelScope.launch {
            HelperService
                .updateUserMetadata(identity, userCanisterId, sanitized)
                .onSuccess {
                    sessionManager.updateUsername(sanitized)
                    _state.update { current ->
                        current.copy(
                            usernameInput = sanitized,
                            initialUsername = sanitized,
                            isUsernameFocused = false,
                            isUsernameValid = true,
                            usernameErrorMessage = null,
                        )
                    }
                }.onFailure { error ->
                    when (error) {
                        is MetadataUpdateError.UsernameTaken -> {
                            _state.update { current ->
                                current.copy(
                                    isUsernameValid = false,
                                    usernameErrorMessage = error.message,
                                )
                            }
                        }

                        is MetadataUpdateError.InvalidUsername -> {
                            _state.update { current ->
                                current.copy(
                                    isUsernameValid = false,
                                    usernameErrorMessage = error.message,
                                )
                            }
                        }

                        else -> {
                            _state.update { current ->
                                current.copy(
                                    usernameInput = previousUsername,
                                    initialUsername = previousUsername,
                                    isUsernameFocused = false,
                                    isUsernameValid = true,
                                    usernameErrorMessage = null,
                                )
                            }
                        }
                    }
                }
        }
    }

    fun revertUsernameChange() {
        _state.update {
            val shouldClear = sanitizeInput(it.usernameInput).isEmpty()
            it.copy(
                usernameInput = if (shouldClear) "" else it.initialUsername,
                isUsernameFocused = false,
                isUsernameValid = true,
                usernameErrorMessage = null,
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
    val usernameErrorMessage: String? = null,
)
