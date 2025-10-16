package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.services.HelperService
import com.yral.shared.rust.service.services.MetadataUpdateError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
) : ViewModel() {
    companion object {
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 15
    }

    private val _state = MutableStateFlow(EditProfileViewState())
    val state: StateFlow<EditProfileViewState> = _state.asStateFlow()

    init {
        val profilePic = sessionManager.profilePic
        val uniqueId = sessionManager.userPrincipal.orEmpty()
        val initialUsername = sanitizedSessionUsername()
        val email =
            sessionManager
                .observeSessionProperties()
                .value
                .emailId
                .orEmpty()
        _state.update {
            it.copy(
                profileImageUrl = profilePic,
                usernameInput = initialUsername,
                initialUsername = initialUsername,
                uniqueId = uniqueId,
                bioInput = "",
                emailId = email,
                isUsernameValid = true,
                usernameErrorMessage = null,
                shouldFocusUsername = false,
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
                shouldFocusUsername = false,
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
            val shouldKeepFocused = it.usernameErrorMessage != null
            when {
                shouldKeepFocused ->
                    it.copy(
                        isUsernameFocused = true,
                        initialUsername = currentInitial,
                        isUsernameValid = false,
                        shouldFocusUsername = false,
                    )

                isFocused ->
                    it.copy(
                        isUsernameFocused = true,
                        initialUsername = currentInitial,
                        isUsernameValid =
                            if (it.usernameErrorMessage == null) {
                                isValidOrEmpty(sanitized)
                            } else {
                                it.isUsernameValid
                            },
                        shouldFocusUsername = false,
                    )

                else ->
                    it.copy(
                        isUsernameFocused = false,
                        initialUsername = sanitizedSessionUsername(),
                        shouldFocusUsername = false,
                    )
            }
        }
    }

    fun onBioChanged(value: String) {
        _state.update { current ->
            current.copy(bioInput = value)
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
                    preferences.putString(PrefKeys.USERNAME.name, sanitized)
                    _state.update { current ->
                        current.copy(
                            usernameInput = sanitized,
                            initialUsername = sanitized,
                            isUsernameFocused = false,
                            isUsernameValid = true,
                            usernameErrorMessage = null,
                            shouldFocusUsername = false,
                        )
                    }
                }.onFailure { error ->
                    when (error) {
                        is MetadataUpdateError.UsernameTaken -> {
                            _state.update { current ->
                                current.copy(
                                    isUsernameValid = false,
                                    usernameErrorMessage = error.message,
                                    shouldFocusUsername = true,
                                    isUsernameFocused = true,
                                )
                            }
                        }

                        is MetadataUpdateError.InvalidUsername -> {
                            _state.update { current ->
                                current.copy(
                                    isUsernameValid = false,
                                    usernameErrorMessage = error.message,
                                    shouldFocusUsername = true,
                                    isUsernameFocused = true,
                                )
                            }
                        }

                        else -> {
                            _state.update { current ->
                                current.copy(
                                    usernameInput = previousUsername,
                                    initialUsername = previousUsername,
                                    isUsernameFocused = true,
                                    isUsernameValid = false,
                                    usernameErrorMessage = error.message,
                                    shouldFocusUsername = true,
                                )
                            }
                        }
                    }
                }
        }
    }

    fun revertUsernameChange() {
        _state.update {
            val defaultUsername = sanitizedSessionUsername()
            it.copy(
                usernameInput = defaultUsername,
                initialUsername = defaultUsername,
                isUsernameFocused = false,
                isUsernameValid = true,
                usernameErrorMessage = null,
                shouldFocusUsername = false,
            )
        }
    }

    fun sanitizedUsername(): String = sanitizeInput(_state.value.usernameInput)

    private fun sanitizeInput(value: String): String = value.trim().removePrefix("@")

    private fun isValidUsername(value: String): Boolean =
        value.length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH &&
            value.all { it.isLetterOrDigit() }

    private fun isValidOrEmpty(value: String): Boolean = value.isEmpty() || isValidUsername(value)

    private fun sanitizedSessionUsername(): String = sanitizeInput(sessionManager.username.orEmpty())
}

data class EditProfileViewState(
    val profileImageUrl: String? = null,
    val usernameInput: String = "",
    val uniqueId: String = "",
    val initialUsername: String = "",
    val bioInput: String = "",
    val emailId: String = "",
    val isUsernameFocused: Boolean = false,
    val isUsernameValid: Boolean = true,
    val usernameErrorMessage: String? = null,
    val shouldFocusUsername: Boolean = false,
)
