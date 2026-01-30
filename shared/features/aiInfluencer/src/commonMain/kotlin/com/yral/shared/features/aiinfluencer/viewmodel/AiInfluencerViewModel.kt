package com.yral.shared.features.aiinfluencer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.usecases.GeneratePromptUseCase
import com.yral.shared.features.aiinfluencer.domain.usecases.ValidateAndGenerateMetadataUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiInfluencerViewModel(
    private val generatePromptUseCase: GeneratePromptUseCase,
    private val validateAndGenerateMetadataUseCase: ValidateAndGenerateMetadataUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AiInfluencerUiState())
    val state: StateFlow<AiInfluencerUiState> = _state.asStateFlow()

    private var requestJob: Job? = null

    fun onPromptChanged(prompt: String) {
        val clamped = prompt.take(PROMPT_CHAR_LIMIT)
        _state.update { current ->
            when (val step = current.step) {
                is AiInfluencerStep.DescriptionEntry ->
                    current.copy(
                        step = step.copy(description = clamped),
                        errorMessage = null,
                    )

                is AiInfluencerStep.PersonaReview ->
                    current.copy(
                        step = step.copy(description = clamped),
                        errorMessage = null,
                    )

                else -> current
            }
        }
    }

    /**
     * Handles back navigation within the flow.
     * @return true if handled internally, false if caller should perform external back.
     */
    fun onBack(): Boolean {
        val currentStep = _state.value.step
        return when (currentStep) {
            is AiInfluencerStep.ProfileDetails -> {
                _state.update {
                    it.copy(
                        step =
                            AiInfluencerStep.PersonaReview(
                                description = currentStep.description,
                                systemInstructions = currentStep.systemInstructions,
                                editedInstructions = currentStep.systemInstructions,
                            ),
                        errorMessage = null,
                    )
                }
                true
            }

            is AiInfluencerStep.PersonaReview -> {
                _state.update {
                    it.copy(
                        step = AiInfluencerStep.DescriptionEntry(description = currentStep.description),
                        errorMessage = null,
                    )
                }
                true
            }

            else -> false
        }
    }

    fun submitPrompt() {
        val description =
            when (val step = _state.value.step) {
                is AiInfluencerStep.DescriptionEntry -> step.description
                is AiInfluencerStep.PersonaReview -> step.description
                else -> return
            }.trim()
        if (description.isBlank()) return

        requestJob?.cancel()
        _state.update {
            it.copy(
                step = AiInfluencerStep.LoadingPrompt(description = description),
                errorMessage = null,
                isImagePickerVisible = false,
            )
        }

        requestJob =
            viewModelScope.launch {
                generatePromptUseCase(GeneratePromptUseCase.Params(prompt = description))
                    .onSuccess { generated ->
                        _state.update {
                            it.copy(
                                step =
                                    AiInfluencerStep.PersonaReview(
                                        description = description,
                                        systemInstructions = generated.systemInstructions,
                                        editedInstructions = generated.systemInstructions,
                                    ),
                                errorMessage = null,
                            )
                        }
                    }.onFailure { throwable ->
                        _state.update {
                            it.copy(
                                step = AiInfluencerStep.DescriptionEntry(description = description),
                                errorMessage = throwable.message ?: DEFAULT_ERROR_MESSAGE,
                            )
                        }
                    }
            }
    }

    fun onPersonaChanged(text: String) {
        _state.update { current ->
            val step = current.step
            if (step is AiInfluencerStep.PersonaReview) {
                current.copy(
                    step = step.copy(editedInstructions = text),
                    errorMessage = null,
                )
            } else {
                current
            }
        }
    }

    fun submitPersona() {
        val currentStep = _state.value.step
        if (currentStep !is AiInfluencerStep.PersonaReview) return

        val edited = currentStep.editedInstructions.trim()
        if (edited.isBlank()) return

        requestJob?.cancel()
        _state.update {
            it.copy(
                step =
                    AiInfluencerStep.LoadingMetadata(
                        description = currentStep.description,
                        systemInstructions = edited,
                    ),
                errorMessage = null,
                isImagePickerVisible = false,
            )
        }

        requestJob =
            viewModelScope.launch {
                validateAndGenerateMetadataUseCase(
                    ValidateAndGenerateMetadataUseCase.Params(systemInstructions = edited),
                ).onSuccess { metadata ->
                    _state.update {
                        it.copy(
                            step = metadata.toProfileDetails(edited),
                            errorMessage = null,
                            isImagePickerVisible = false,
                        )
                    }
                }.onFailure { throwable ->
                    _state.update {
                        it.copy(
                            step = currentStep,
                            errorMessage = throwable.message ?: DEFAULT_ERROR_MESSAGE,
                        )
                    }
                }
            }
    }

    fun onProfileNameChanged(name: String) {
        _state.update { current ->
            when (val step = current.step) {
                is AiInfluencerStep.ProfileDetails -> current.copy(step = step.copy(name = name))
                else -> current
            }
        }
    }

    fun onProfileDescriptionChanged(description: String) {
        val clamped = description.take(PROMPT_CHAR_LIMIT)
        _state.update { current ->
            when (val step = current.step) {
                is AiInfluencerStep.ProfileDetails -> current.copy(step = step.copy(description = clamped))
                else -> current
            }
        }
    }

    fun onAvatarUpdated(newAvatar: String) {
        _state.update { current ->
            when (val step = current.step) {
                is AiInfluencerStep.ProfileDetails ->
                    current.copy(
                        step = step.copy(avatarUrl = newAvatar),
                        isImagePickerVisible = false,
                    )
                else -> current
            }
        }
    }

    fun openImagePicker() {
        _state.update { it.copy(isImagePickerVisible = true) }
    }

    fun dismissImagePicker() {
        _state.update { it.copy(isImagePickerVisible = false) }
    }

    companion object {
        private const val PROMPT_CHAR_LIMIT = 200
        private const val DEFAULT_ERROR_MESSAGE = "Something went wrong. Please try again."
    }
}

data class AiInfluencerUiState(
    val step: AiInfluencerStep = AiInfluencerStep.DescriptionEntry(),
    val errorMessage: String? = null,
    val isImagePickerVisible: Boolean = false,
)

sealed interface AiInfluencerStep {
    data class DescriptionEntry(
        val description: String = "",
    ) : AiInfluencerStep

    data class LoadingPrompt(
        val description: String,
    ) : AiInfluencerStep

    data class PersonaReview(
        val description: String,
        val systemInstructions: String,
        val editedInstructions: String,
    ) : AiInfluencerStep

    data class LoadingMetadata(
        val description: String,
        val systemInstructions: String,
    ) : AiInfluencerStep

    data class ProfileDetails(
        val systemInstructions: String,
        val name: String,
        val displayName: String,
        val description: String,
        val avatarUrl: String,
        val initialGreeting: String,
        val suggestedMessages: List<String>,
        val personalityTraits: Map<String, String>,
        val isValid: Boolean,
        val validationReason: String,
        val category: String,
    ) : AiInfluencerStep
}

private fun GeneratedInfluencerMetadata.toProfileDetails(systemInstructions: String): AiInfluencerStep.ProfileDetails =
    AiInfluencerStep.ProfileDetails(
        systemInstructions = systemInstructions,
        name = name,
        displayName = displayName,
        description = description,
        avatarUrl = avatarUrl,
        initialGreeting = initialGreeting,
        suggestedMessages = suggestedMessages,
        personalityTraits = personalityTraits,
        isValid = isValid,
        validationReason = reason,
        category = category,
    )
