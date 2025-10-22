package com.yral.shared.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.profile.domain.UploadProfileImageParams
import com.yral.shared.features.profile.domain.UploadProfileImageUseCase
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4Params
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4UseCase
import com.yral.shared.rust.service.domain.usecases.UpdateProfileDetailsParams
import com.yral.shared.rust.service.domain.usecases.UpdateProfileDetailsUseCase
import com.yral.shared.rust.service.services.HelperService
import com.yral.shared.rust.service.services.MetadataUpdateError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class EditProfileViewModel(
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
    private val getProfileDetailsV4UseCase: GetProfileDetailsV4UseCase,
    private val updateProfileDetailsUseCase: UpdateProfileDetailsUseCase,
    private val uploadProfileImageUseCase: UploadProfileImageUseCase,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val logger = Logger.withTag("EditProfileViewModel")
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
        _state.update {
            it.copy(
                profileImageUrl = profilePic,
                usernameInput = initialUsername,
                initialUsername = initialUsername,
                uniqueId = uniqueId,
                bioInput = "",
                initialBio = "",
                isUsernameValid = true,
                usernameErrorMessage = null,
                shouldFocusUsername = false,
            )
        }

        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { properties -> properties.emailId },
                    defaultValue = "",
                ).collect { email ->
                    _state.update { current -> current.copy(emailId = email) }
                }
        }

        fetchProfileDetails()
    }

    private fun fetchProfileDetails() {
        val principalText = sessionManager.userPrincipal ?: return
        val principal = principalText
        viewModelScope.launch {
            getProfileDetailsV4UseCase(
                GetProfileDetailsV4Params(
                    principal = principal,
                    targetPrincipal = principal,
                ),
            ).onSuccess { details ->
                val bio = sanitizeBio(details.bio.orEmpty())
                val profileImage = details.profilePictureUrl
                _state.update { current ->
                    if (current.initialBio.isNotEmpty()) {
                        current
                    } else {
                        current.copy(
                            bioInput = bio,
                            initialBio = bio,
                            profileImageUrl = profileImage ?: current.profileImageUrl,
                        )
                    }
                }
                if (profileImage != null) {
                    sessionManager.updateProfilePicture(profileImage)
                    preferences.putString(PrefKeys.PROFILE_PIC.name, profileImage)
                }
            }.onFailure { error ->
                logger.e { "Failed to fetch profile details: ${error.message}" }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun uploadProfileImage(imageBytes: ByteArray) {
        if (_state.value.isUploadingProfileImage) {
            return
        }
        viewModelScope.launch {
            _state.update { current ->
                current.copy(isUploadingProfileImage = true, profileImageUploadError = null)
            }

            val base64 =
                try {
                    encodeToBase64(imageBytes)
                } catch (error: Throwable) {
                    logger.e { "Failed to encode profile image: ${error.message}" }
                    crashlyticsManager.recordException(error as? Exception ?: Exception(error))
                    _state.update { current ->
                        current.copy(
                            isUploadingProfileImage = false,
                            profileImageUploadError = error.message ?: "Unable to process image",
                        )
                    }
                    return@launch
                }

            uploadProfileImageUseCase(UploadProfileImageParams(base64))
                .onSuccess { imageUrl ->
                    sessionManager.updateProfilePicture(imageUrl)
                    preferences.putString(PrefKeys.PROFILE_PIC.name, imageUrl)
                    _state.update { current ->
                        current.copy(
                            profileImageUrl = imageUrl,
                            isUploadingProfileImage = false,
                            profileImageUploadError = null,
                        )
                    }
                }.onFailure { error ->
                    logger.e { "Failed to upload profile image: ${error.message}" }
                    _state.update { current ->
                        current.copy(
                            isUploadingProfileImage = false,
                            profileImageUploadError = error.message ?: "Failed to upload profile picture",
                        )
                    }
                }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeToBase64(bytes: ByteArray): String = Base64.Default.encode(bytes)

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

    fun clearUsernameInput() {
        _state.update {
            it.copy(
                usernameInput = "",
                isUsernameValid = true,
                usernameErrorMessage = null,
                shouldFocusUsername = true,
            )
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

    @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount", "TooGenericExceptionCaught")
    fun saveProfileChanges() {
        val sanitizedUsername = sanitizeInput(_state.value.usernameInput)
        val sanitizedBio = sanitizeBio(_state.value.bioInput)
        val userPrincipalText = sessionManager.userPrincipal ?: return
        val principal = userPrincipalText
        val identity = sessionManager.identity
        val userCanisterId = sessionManager.canisterID
        val previousUsername = _state.value.initialUsername
        val previousBio = _state.value.initialBio
        val shouldUpdateUsername = previousUsername != sanitizedUsername
        val shouldUpdateBio = previousBio != sanitizedBio

        _state.update { current ->
            current.copy(usernameInput = sanitizedUsername, bioInput = sanitizedBio)
        }

        if (shouldUpdateUsername && (identity == null || userCanisterId == null)) {
            logger.e { "Cannot update username without identity data or canister id" }
            return
        }

        if (!shouldUpdateUsername && !shouldUpdateBio) {
            _state.update { current ->
                current.copy(
                    usernameInput = sanitizedUsername,
                    initialUsername = sanitizedUsername,
                    bioInput = sanitizedBio,
                    initialBio = sanitizedBio,
                    isUsernameFocused = false,
                    isUsernameValid = true,
                    usernameErrorMessage = null,
                    shouldFocusUsername = false,
                )
            }
            return
        }

        _state.update { current ->
            current.copy(isSavingProfile = true)
        }

        viewModelScope.launch {
            try {
                if (shouldUpdateUsername) {
                    val identityData = identity
                    val canisterId = userCanisterId
                    if (identityData == null || canisterId == null) {
                        logger.e { "Cannot update username without identity data or canister id" }
                        _state.update { current ->
                            current.copy(usernameInput = previousUsername)
                        }
                        return@launch
                    }
                    HelperService
                        .updateUserMetadata(
                            identityData,
                            canisterId,
                            sanitizedUsername,
                        ).onFailure { error ->
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
                            return@launch
                        }.onSuccess {
                            sessionManager.updateUsername(sanitizedUsername)
                            preferences.putString(PrefKeys.USERNAME.name, sanitizedUsername)
                            _state.update { current ->
                                current.copy(
                                    usernameInput = sanitizedUsername,
                                    initialUsername = sanitizedUsername,
                                    isUsernameFocused = false,
                                    isUsernameValid = true,
                                    usernameErrorMessage = null,
                                    shouldFocusUsername = false,
                                )
                            }
                        }
                }

                if (!shouldUpdateUsername) {
                    _state.update { current ->
                        current.copy(
                            usernameInput = sanitizedUsername,
                            initialUsername = sanitizedUsername,
                            isUsernameFocused = false,
                            isUsernameValid = true,
                            usernameErrorMessage = null,
                            shouldFocusUsername = false,
                        )
                    }
                }

                if (shouldUpdateBio) {
                    updateProfileDetailsUseCase(
                        UpdateProfileDetailsParams(
                            principal = principal,
                            bio = sanitizedBio.takeUnless { it.isEmpty() },
                        ),
                    ).onFailure { error ->
                        logger.e { "Failed to update bio: ${error.message}" }
                        _state.update { current ->
                            current.copy(
                                bioInput = previousBio,
                                initialBio = previousBio,
                            )
                        }
                        return@launch
                    }.onSuccess {
                        _state.update { current ->
                            current.copy(
                                bioInput = sanitizedBio,
                                initialBio = sanitizedBio,
                            )
                        }
                    }
                } else {
                    _state.update { current ->
                        current.copy(
                            bioInput = sanitizedBio,
                            initialBio = sanitizedBio,
                        )
                    }
                }
            } catch (error: Throwable) {
                logger.e { "Unexpected failure saving profile: ${error.message}" }
                _state.update {
                    it.copy(
                        usernameInput = previousUsername,
                        initialUsername = previousUsername,
                        bioInput = previousBio,
                        initialBio = previousBio,
                        isUsernameFocused = false,
                        isUsernameValid = true,
                        usernameErrorMessage = null,
                        shouldFocusUsername = false,
                    )
                }
            } finally {
                _state.update { current ->
                    current.copy(isSavingProfile = false)
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

    fun discardUnsavedProfileEdits() {
        _state.update { current ->
            val fallbackUsername = sanitizedSessionUsername()
            current.copy(
                usernameInput = current.initialUsername.ifEmpty { fallbackUsername },
                bioInput = current.initialBio,
                isUsernameFocused = false,
                isUsernameValid = true,
                usernameErrorMessage = null,
                shouldFocusUsername = false,
                isSavingProfile = false,
            )
        }
    }

    fun sanitizedUsername(): String = sanitizeInput(_state.value.usernameInput)

    private fun sanitizeInput(value: String): String = value.trim().removePrefix("@")

    private fun sanitizeBio(value: String): String = value.trim()

    private fun isValidUsername(value: String): Boolean =
        value.length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH &&
            value.all { it.isLetterOrDigit() }

    private fun isValidOrEmpty(value: String): Boolean = value.isEmpty() || isValidUsername(value)

    private fun sanitizedSessionUsername(): String = sanitizeInput(sessionManager.username.orEmpty())
}

data class EditProfileViewState(
    val profileImageUrl: String? = null,
    val isUploadingProfileImage: Boolean = false,
    val profileImageUploadError: String? = null,
    val usernameInput: String = "",
    val uniqueId: String = "",
    val initialUsername: String = "",
    val bioInput: String = "",
    val initialBio: String = "",
    val emailId: String = "",
    val isUsernameFocused: Boolean = false,
    val isUsernameValid: Boolean = true,
    val usernameErrorMessage: String? = null,
    val shouldFocusUsername: Boolean = false,
    val isSavingProfile: Boolean = false,
)
