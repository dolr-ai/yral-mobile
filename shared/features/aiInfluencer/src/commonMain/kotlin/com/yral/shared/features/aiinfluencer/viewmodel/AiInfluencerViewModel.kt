package com.yral.shared.features.aiinfluencer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.usecases.CreateInfluencerUseCase
import com.yral.shared.features.aiinfluencer.domain.usecases.GeneratePromptUseCase
import com.yral.shared.features.aiinfluencer.domain.usecases.ValidateAndGenerateMetadataUseCase
import com.yral.shared.features.auth.domain.useCases.CreateAiAccountUseCase
import com.yral.shared.http.httpPost
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.domain.usecases.AcceptNewUserRegistrationV2Params
import com.yral.shared.rust.service.domain.usecases.AcceptNewUserRegistrationV2UseCase
import com.yral.shared.rust.service.domain.usecases.UpdateProfileDetailsParams
import com.yral.shared.rust.service.domain.usecases.UpdateProfileDetailsUseCase
import com.yral.shared.rust.service.services.HelperService
import com.yral.shared.rust.service.utils.SignedMessage
import com.yral.shared.rust.service.utils.authenticateWithNetwork
import com.yral.shared.rust.service.utils.delegatedIdentityWireToJson
import com.yral.shared.rust.service.utils.getSessionFromIdentity
import com.yral.shared.rust.service.utils.signMessageWithIdentity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
@Suppress("LongParameterList")
class AiInfluencerViewModel(
    private val generatePromptUseCase: GeneratePromptUseCase,
    private val validateAndGenerateMetadataUseCase: ValidateAndGenerateMetadataUseCase,
    private val createAiAccountUseCase: CreateAiAccountUseCase,
    private val acceptNewUserRegistrationV2UseCase: AcceptNewUserRegistrationV2UseCase,
    private val createInfluencerUseCase: CreateInfluencerUseCase,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
    private val json: Json,
    private val botIdentityStorage: BotIdentityStorage,
    private val updateProfileDetailsUseCase: UpdateProfileDetailsUseCase,
    private val httpClient: HttpClient,
) : ViewModel() {
    private val logger = Logger.withTag("AiInfluencerViewModel")
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

    @Suppress("LongMethod")
    fun createBotAccount(onSuccess: () -> Unit) {
        val currentStep = _state.value.step as? AiInfluencerStep.ProfileDetails
        val identity = sessionManager.identity
        val principal = sessionManager.userPrincipal
        if (currentStep == null || identity == null || principal.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = DEFAULT_ERROR_MESSAGE) }
            return
        }

        val signedMessage: SignedMessage =
            runCatching {
                signMessageWithIdentity(
                    identity = identity,
                    message = BOT_CREATE_MESSAGE,
                )
            }.getOrElse {
                _state.update { state ->
                    state.copy(
                        errorMessage = "Unable to sign request. Please try again.",
                    )
                }
                return
            }

        requestJob?.cancel()
        _state.update { it.copy(isBotCreationLoading = true, errorMessage = null) }

        requestJob =
            viewModelScope.launch {
                createAiAccountUseCase(
                    CreateAiAccountUseCase.Params(
                        userPrincipal = principal,
                        signature = signedMessage.sig ?: ByteArray(0),
                        publicKey = signedMessage.publicKey ?: ByteArray(0),
                        signedMessage = ByteArray(0),
                        ingressExpirySecs = signedMessage.ingressExpirySecs,
                        ingressExpiryNanos = signedMessage.ingressExpiryNanos,
                        delegations = signedMessage.delegations,
                    ),
                ).onSuccess { delegatedIdentityBytes ->
                    logger.d { "createAiAccount: success, bytes=${delegatedIdentityBytes.size}" }
                    val newBotPrincipal =
                        runCatching { getSessionFromIdentity(delegatedIdentityBytes).userPrincipalId }
                            .getOrElse {
                                logger.e { "createAiAccount: failed to parse bot principal ${it.message}" }
                                _state.update {
                                    it.copy(
                                        isBotCreationLoading = false,
                                        errorMessage =
                                            "Unable to parse bot principal. Please try again.",
                                    )
                                }
                                return@launch
                            }

                    acceptNewUserRegistrationV2UseCase(
                        AcceptNewUserRegistrationV2Params(
                            principal = principal,
                            newPrincipal = newBotPrincipal,
                            authenticated = true,
                            mainAccount = principal,
                        ),
                    ).onSuccess {
                        logger.d { "accept_new_user_registration_v2: success for bot=$newBotPrincipal" }
                        completeBotSetup(
                            botPrincipal = newBotPrincipal,
                            botIdentity = delegatedIdentityBytes,
                            profileDetails = currentStep,
                        ).onSuccess {
                            _state.update { it.copy(isBotCreationLoading = false) }
                            onSuccess()
                        }.onFailure { error ->
                            _state.update {
                                it.copy(
                                    isBotCreationLoading = false,
                                    errorMessage = error.message ?: DEFAULT_ERROR_MESSAGE,
                                )
                            }
                        }
                    }.onFailure { throwable ->
                        logger.e { "accept_new_user_registration_v2: failed ${throwable.message}" }
                        _state.update {
                            it.copy(
                                isBotCreationLoading = false,
                                errorMessage = throwable.message ?: DEFAULT_ERROR_MESSAGE,
                            )
                        }
                    }
                }.onFailure { throwable ->
                    logger.e { "createAiAccount: failed ${throwable.message}" }
                    val serverMessage = extractServerMessage(throwable)
                    _state.update {
                        it.copy(
                            isBotCreationLoading = false,
                            errorMessage = serverMessage ?: throwable.message ?: DEFAULT_ERROR_MESSAGE,
                        )
                    }
                }
            }
    }

    companion object {
        private const val PROMPT_CHAR_LIMIT = 200
        private const val DEFAULT_ERROR_MESSAGE = "Something went wrong. Please try again."
        private const val BOT_CREATE_MESSAGE = "yral_auth_v2_create_ai_account"
    }

    @Suppress("LongMethod")
    private suspend fun completeBotSetup(
        botPrincipal: String,
        botIdentity: ByteArray,
        profileDetails: AiInfluencerStep.ProfileDetails,
    ): Result<Unit> =
        runCatching {
            logger.d { "bot_setup: start for bot=$botPrincipal" }
            // Authenticate with network using bot identity to get canister info
            val canisterData =
                authenticateWithNetwork(botIdentity).also {
                    logger.d { "bot_setup: authenticate_with_network success canister=${it.canisterId}" }
                }
            // Update username via set_user_metadata
            runCatching {
                HelperService
                    .updateUserMetadata(
                        identityData = botIdentity,
                        userCanisterId = canisterData.canisterId,
                        userName = profileDetails.name,
                    ).getOrThrow()
            }.onSuccess {
                logger.d { "bot_setup: set_user_metadata success" }
            }.onFailure { throwable ->
                // Username collisions shouldn't block the rest of the flow
                logger.w { "bot_setup: set_user_metadata failed (continuing) - ${throwable.message}" }
            }

            // Upload avatar (download then upload)
            val avatarBytes =
                downloadAvatar(profileDetails.avatarUrl).also {
                    logger.d { "bot_setup: downloaded avatar bytes=${it.size}" }
                }
            val uploadedAvatarUrl =
                uploadProfileImage(
                    imageBase64 = avatarBytes.encodeBase64(),
                    identityBase64 = botIdentity.encodeBase64(),
                ).also {
                    logger.d { "bot_setup: upload avatar success url=$it" }
                }

            // Update bio and profile picture
            val originalIdentity = sessionManager.identity
            HelperService.initServiceFactories(botIdentity)
            try {
                updateProfileDetailsUseCase(
                    UpdateProfileDetailsParams(
                        principal = botPrincipal,
                        bio = profileDetails.description,
                        profilePictureUrl = uploadedAvatarUrl,
                    ),
                ).also {
                    logger.d { "bot_setup: update_profile_details_v2 success" }
                }
            } finally {
                originalIdentity?.let { HelperService.initServiceFactories(it) }
            }

            // Create influencer record in backend
            createInfluencerUseCase(
                CreateInfluencerUseCase.Params(
                    request =
                        com.yral.shared.features.aiinfluencer.domain.models.CreatedInfluencer(
                            name = profileDetails.name,
                            displayName = profileDetails.displayName,
                            description = profileDetails.description,
                            systemInstructions = profileDetails.systemInstructions,
                            initialGreeting = profileDetails.initialGreeting,
                            suggestedMessages = profileDetails.suggestedMessages,
                            personalityTraits = profileDetails.personalityTraits,
                            category = profileDetails.category,
                            avatarUrl = uploadedAvatarUrl,
                            isNsfw = profileDetails.isNsfw,
                            botPrincipalId = botPrincipal,
                            parentPrincipalId = sessionManager.userPrincipal.orEmpty(),
                        ),
                ),
            ).also {
                logger.d { "bot_setup: create influencer success" }
            }

            botIdentityStorage.saveBotIdentity(
                principal = botPrincipal,
                identity = botIdentity,
            )
            logger.d { "bot_setup: completed" }
            setActiveBotSession(
                botPrincipal = botPrincipal,
                botIdentity = botIdentity,
                canisterData = canisterData,
                profileDetails = profileDetails,
                profilePicUrl = uploadedAvatarUrl,
            )
        }

    private suspend fun uploadProfileImage(
        imageBase64: String,
        identityBase64: String,
    ): String {
        val identityWireJson = delegatedIdentityWireToJson(Base64.decode(identityBase64))
        val delegatedIdentityWire = json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)

        val response =
            httpPost<UploadProfileImageResponse>(httpClient, json) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = OFF_CHAIN_BASE_URL
                    path(UPLOAD_PROFILE_ENDPOINT)
                }
                setBody(
                    UploadProfileImageRequestBody(
                        delegatedIdentityWire = delegatedIdentityWire,
                        imageData = imageBase64,
                    ),
                )
            }

        return response.profileImageUrl
    }

    private suspend fun downloadAvatar(url: String): ByteArray = httpClient.get(url).body()

    private suspend fun setActiveBotSession(
        botPrincipal: String,
        botIdentity: ByteArray,
        canisterData: com.yral.shared.rust.service.utils.CanisterData,
        profileDetails: AiInfluencerStep.ProfileDetails,
        profilePicUrl: String,
    ) {
        val mainIdentitySnapshot = sessionManager.identity
        val mainPrincipalSnapshot = sessionManager.userPrincipal
        // Switch in-memory session to bot for immediate use
        HelperService.initServiceFactories(botIdentity)
        val botSession =
            Session(
                identity = botIdentity,
                canisterId = canisterData.canisterId,
                userPrincipal = botPrincipal,
                profilePic = profilePicUrl,
                username = profileDetails.name,
                bio = profileDetails.description,
                isCreatedFromServiceCanister = canisterData.isCreatedFromServiceCanister,
                isBotAccount = true,
            )
        sessionManager.updateState(SessionState.SignedIn(session = botSession))
        // Persist so subsequent launches continue as bot until switched
        preferences.putBytes(PrefKeys.IDENTITY.name, botIdentity)
        preferences.putString(PrefKeys.CANISTER_ID.name, canisterData.canisterId)
        preferences.putString(PrefKeys.USER_PRINCIPAL.name, botPrincipal)
        preferences.putString(PrefKeys.PROFILE_PIC.name, profilePicUrl)
        preferences.putString(PrefKeys.USERNAME.name, profileDetails.name)
        // Preserve main account identity/principal if not already stored
        mainIdentitySnapshot?.let { mainIdentity ->
            preferences.putBytes(PrefKeys.MAIN_IDENTITY.name, mainIdentity)
        }
        mainPrincipalSnapshot?.let { mainPrincipal ->
            preferences.putString(PrefKeys.MAIN_PRINCIPAL.name, mainPrincipal)
        }
        preferences.putBoolean(
            PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name,
            canisterData.isCreatedFromServiceCanister,
        )
        logger.d { "bot_setup: session switched to bot $botPrincipal" }
    }

    private suspend fun extractServerMessage(throwable: Throwable): String? =
        runCatching {
            val response = (throwable as? Exception)?.cause as? ResponseException
            response?.response?.bodyAsText()
        }.getOrNull()
}

class BotIdentityStorage(
    private val preferences: Preferences,
    private val json: Json,
) {
    suspend fun saveBotIdentity(
        principal: String,
        identity: ByteArray,
    ) {
        val existing =
            preferences
                .getString(PrefKeys.BOT_IDENTITIES.name)
                ?.let { runCatching { json.decodeFromString<List<BotIdentityEntry>>(it) }.getOrNull() }
                ?: emptyList()
        val updated =
            existing
                .filterNot { it.principal == principal } +
                BotIdentityEntry(principal = principal, identity = identity.encodeBase64())
        val encoded = json.encodeToString(updated)
        preferences.putString(PrefKeys.BOT_IDENTITIES.name, encoded)
    }

    @Serializable
    private data class BotIdentityEntry(
        val principal: String,
        val identity: String,
    )
}

@Serializable
private data class UploadProfileImageRequestBody(
    @SerialName("delegated_identity_wire")
    val delegatedIdentityWire: KotlinDelegatedIdentityWire,
    @SerialName("image_data")
    val imageData: String,
)

@Serializable
private data class UploadProfileImageResponse(
    @SerialName("profile_image_url")
    val profileImageUrl: String,
)

private const val UPLOAD_PROFILE_ENDPOINT = "/api/v1/user/profile-image"

data class AiInfluencerUiState(
    val step: AiInfluencerStep = AiInfluencerStep.DescriptionEntry(),
    val errorMessage: String? = null,
    val isImagePickerVisible: Boolean = false,
    val isBotCreationLoading: Boolean = false,
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
        val isNsfw: Boolean = false,
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
        isNsfw = isNsfw,
    )
