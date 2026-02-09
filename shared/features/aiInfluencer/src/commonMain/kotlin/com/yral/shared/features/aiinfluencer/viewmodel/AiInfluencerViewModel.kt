package com.yral.shared.features.aiinfluencer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.utils.generateUsernameFromPrincipal
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.usecases.CreateInfluencerUseCase
import com.yral.shared.features.aiinfluencer.domain.usecases.GeneratePromptUseCase
import com.yral.shared.features.aiinfluencer.domain.usecases.ValidateAndGenerateMetadataUseCase
import com.yral.shared.features.auth.domain.useCases.CreateAiAccountUseCase
import com.yral.shared.features.uploadvideo.domain.models.ImageData
import com.yral.shared.features.uploadvideo.domain.models.ImageInput
import com.yral.shared.features.uploadvideo.presentation.BotVideoGenManager
import com.yral.shared.http.httpPost
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.domain.usecases.AcceptNewUserRegistrationV2Params
import com.yral.shared.rust.service.domain.usecases.AcceptNewUserRegistrationV2UseCase
import com.yral.shared.rust.service.domain.usecases.UpdateProfileDetailsParams
import com.yral.shared.rust.service.domain.usecases.UpdateProfileDetailsUseCase
import com.yral.shared.rust.service.services.HelperService
import com.yral.shared.rust.service.services.MetadataUpdateError
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
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
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
    private val botVideoGenCoordinator: BotVideoGenManager,
) : ViewModel() {
    private val logger = Logger.withTag("AiInfluencerViewModel")
    private val _state = MutableStateFlow(AiInfluencerUiState())
    val state: StateFlow<AiInfluencerUiState> = _state.asStateFlow()

    fun resetFlow() {
        clearBotCreationProgress()
        _state.value = AiInfluencerUiState()
    }

    private var requestJob: Job? = null
    private var botCreationProgress: BotCreationProgress? = null

    fun onPromptChanged(prompt: String) {
        val clamped = prompt.take(PROMPT_CHAR_LIMIT)
        _state.update { current ->
            when (val step = current.step) {
                is AiInfluencerStep.DescriptionEntry ->
                    current.copy(
                        promptInput = clamped,
                        step = step.copy(description = clamped),
                        errorMessage = null,
                    )

                is AiInfluencerStep.PersonaReview ->
                    current.copy(
                        promptInput = clamped,
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
                clearBotCreationProgress()
                val prompt = _state.value.promptInput
                _state.update {
                    it.copy(
                        step =
                            AiInfluencerStep.PersonaReview(
                                description = prompt.ifBlank { currentStep.description },
                                systemInstructions = currentStep.systemInstructions,
                                editedInstructions = currentStep.systemInstructions,
                            ),
                        errorMessage = null,
                    )
                }
                true
            }

            is AiInfluencerStep.PersonaReview -> {
                val prompt = _state.value.promptInput
                _state.update {
                    it.copy(
                        step =
                            AiInfluencerStep.DescriptionEntry(
                                description = prompt.ifBlank { currentStep.description },
                            ),
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
                promptInput = description,
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
                    if (metadata.isValid) {
                        _state.update {
                            it.copy(
                                step = metadata.toProfileDetails(edited),
                                errorMessage = null,
                                isImagePickerVisible = false,
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                step = currentStep,
                                errorMessage = metadata.reason.ifBlank { DEFAULT_ERROR_MESSAGE },
                            )
                        }
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

    fun onAvatarSelected(bytes: ByteArray) {
        _state.update { current ->
            when (val step = current.step) {
                is AiInfluencerStep.ProfileDetails ->
                    current.copy(
                        step = step.copy(avatarBytes = bytes),
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
        val profileKey = currentStep.progressKey()
        val progress =
            botCreationProgress?.takeIf { it.profileKey == profileKey } ?: BotCreationProgress(profileKey)
        botCreationProgress = progress

        requestJob =
            viewModelScope.launch {
                runCatching {
                    val delegatedIdentityBytes =
                        progress.botIdentity
                            ?: createAiAccountUseCase(
                                CreateAiAccountUseCase.Params(
                                    userPrincipal = principal,
                                    signature = signedMessage.sig ?: ByteArray(0),
                                    publicKey = signedMessage.publicKey ?: ByteArray(0),
                                    signedMessage = ByteArray(0),
                                    ingressExpirySecs = signedMessage.ingressExpirySecs,
                                    ingressExpiryNanos = signedMessage.ingressExpiryNanos,
                                    delegations = signedMessage.delegations,
                                ),
                            ).getOrThrow().also {
                                logger.d { "createAiAccount: success, bytes=${it.size}" }
                                progress.botIdentity = it
                            }

                    val newBotPrincipal =
                        progress.botPrincipal
                            ?: runCatching { getSessionFromIdentity(delegatedIdentityBytes).userPrincipalId }
                                .getOrElse {
                                    throw YralException("Unable to parse bot principal. Please try again.")
                                }.also { progress.botPrincipal = it }

                    if (!progress.registrationAccepted) {
                        acceptNewUserRegistrationV2UseCase(
                            AcceptNewUserRegistrationV2Params(
                                principal = principal,
                                newPrincipal = newBotPrincipal,
                                authenticated = true,
                                mainAccount = principal,
                            ),
                        ).getOrThrow()
                        logger.d { "accept_new_user_registration_v2: success for bot=$newBotPrincipal" }
                        progress.registrationAccepted = true
                    }

                    completeBotSetup(
                        progress = progress,
                        botPrincipal = newBotPrincipal,
                        botIdentity = delegatedIdentityBytes,
                        profileDetails = currentStep,
                    ).getOrThrow()
                }.onSuccess {
                    _state.update { it.copy(isBotCreationLoading = false) }
                    clearBotCreationProgress()
                    onSuccess()
                }.onFailure { throwable ->
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
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 15
        private const val RETRY_SUFFIX_LENGTH = 3
        private val PNG_MAGIC =
            byteArrayOf(
                0x89.toByte(),
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A,
            )
        private val JPEG_MAGIC =
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(),
                0xFF.toByte(),
            )
        private const val MIME_TYPE_PNG = "image/png"
        private const val MIME_TYPE_JPEG = "image/jpeg"
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun completeBotSetup(
        progress: BotCreationProgress,
        botPrincipal: String,
        botIdentity: ByteArray,
        profileDetails: AiInfluencerStep.ProfileDetails,
    ): Result<com.yral.shared.rust.service.utils.CanisterData> =
        runCatching {
            logger.d { "bot_setup: start for bot=$botPrincipal" }
            // Authenticate with network using bot identity to get canister info
            val canisterData =
                progress.canisterData
                    ?: authenticateWithNetwork(botIdentity).also {
                        logger.d { "bot_setup: authenticate_with_network success canister=${it.canisterId}" }
                        progress.canisterData = it
                    }
            // Update username via set_user_metadata
            val fallbackUsername =
                resolveUsername(preferred = null, principal = botPrincipal)
                    ?: generateUsernameFromPrincipal(botPrincipal)
            val usernameResult =
                progress.usernameResult
                    ?: tryUpdateUsername(
                        botIdentity = botIdentity,
                        canisterId = canisterData.canisterId,
                        requestedUsername = profileDetails.name,
                        botPrincipal = botPrincipal,
                        fallbackUsername = fallbackUsername,
                    ).also { progress.usernameResult = it }
            val usernameForCreateApi = usernameResult.username
            val usernameUpdated = usernameResult.updated

            // Upload avatar (download then upload)
            val avatarBytes =
                progress.avatarBytes
                    ?: (
                        profileDetails.avatarBytes
                            ?: downloadAvatar(profileDetails.avatarUrl).also {
                                logger.d { "bot_setup: downloaded avatar bytes=${it.size}" }
                            }
                    ).also { progress.avatarBytes = it }
            val uploadedAvatarUrl =
                progress.uploadedAvatarUrl
                    ?: uploadProfileImage(
                        imageBase64 = avatarBytes.encodeBase64(),
                        identityBase64 = botIdentity.encodeBase64(),
                    ).also {
                        logger.d { "bot_setup: upload avatar success url=$it" }
                        progress.uploadedAvatarUrl = it
                    }

            // Update bio and profile picture
            if (!progress.profileUpdated) {
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
                    progress.profileUpdated = true
                } finally {
                    originalIdentity?.let { HelperService.initServiceFactories(it) }
                }
            }

            // Create influencer record in backend
            val createdInfluencer =
                progress.createdInfluencer
                    ?: runCatching {
                        createInfluencerUseCase(
                            CreateInfluencerUseCase.Params(
                                request =
                                    com.yral.shared.features.aiinfluencer.domain.models.CreatedInfluencer(
                                        name = usernameForCreateApi,
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
                        ).getOrThrow()
                    }.getOrElse { throwable ->
                        val serverMessage = extractServerMessage(throwable)
                        logger.e {
                            "bot_setup: create influencer failed ${serverMessage ?: throwable.message}"
                        }
                        throw YralException(serverMessage ?: throwable.message ?: DEFAULT_ERROR_MESSAGE)
                    }.also {
                        logger.d { "bot_setup: create influencer success" }
                        progress.createdInfluencer = it
                    }

            if (!progress.finalized) {
                botIdentityStorage.saveBotIdentity(
                    principal = botPrincipal,
                    identity = botIdentity,
                    username = usernameForCreateApi.takeIf { usernameUpdated },
                )
                setActiveBotSession(
                    botPrincipal = botPrincipal,
                    botIdentity = botIdentity,
                    canisterData = canisterData,
                    profileDetails = profileDetails,
                    profilePicUrl = uploadedAvatarUrl,
                    displayUsername = if (usernameUpdated) usernameForCreateApi else null,
                )

                val starterPrompt = createdInfluencer.starterVideoPrompt
                if (!starterPrompt.isNullOrBlank()) {
                    botVideoGenCoordinator.enqueueGeneration(
                        botPrincipal = botPrincipal,
                        prompt = starterPrompt,
                        imageData =
                            ImageData.Base64(
                                ImageInput(
                                    data = avatarBytes.encodeBase64(),
                                    mimeType = resolveImageMimeType(avatarBytes),
                                ),
                            ),
                    )
                }
                progress.finalized = true
                logger.d { "bot_setup: completed" }
            }
            canisterData
        }

    private suspend fun tryUpdateUsername(
        botIdentity: ByteArray,
        canisterId: String,
        requestedUsername: String,
        botPrincipal: String,
        fallbackUsername: String,
    ): UsernameUpdateResult {
        val normalizedUsername = normalizeBotUsername(requestedUsername, botPrincipal)
        return runCatching {
            HelperService
                .updateUserMetadata(
                    identityData = botIdentity,
                    userCanisterId = canisterId,
                    userName = normalizedUsername,
                ).getOrThrow()
            logger.d { "bot_setup: set_user_metadata success" }
            UsernameUpdateResult(username = normalizedUsername, updated = true)
        }.getOrElse { throwable ->
            if (isUsernameTakenError(throwable)) {
                val retryUsername = buildRetryUsername(normalizedUsername, botPrincipal)
                runCatching {
                    HelperService
                        .updateUserMetadata(
                            identityData = botIdentity,
                            userCanisterId = canisterId,
                            userName = retryUsername,
                        ).getOrThrow()
                    logger.d { "bot_setup: set_user_metadata retry success username=$retryUsername" }
                    UsernameUpdateResult(username = retryUsername, updated = true)
                }.getOrElse { retryThrowable ->
                    logger.w {
                        "bot_setup: set_user_metadata retry failed (continuing) - ${retryThrowable.message}"
                    }
                    UsernameUpdateResult(username = fallbackUsername, updated = false)
                }
            } else {
                logger.w { "bot_setup: set_user_metadata failed (continuing) - ${throwable.message}" }
                UsernameUpdateResult(username = fallbackUsername, updated = false)
            }
        }
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

    private fun resolveImageMimeType(bytes: ByteArray): String {
        val isPng =
            bytes.size >= PNG_MAGIC.size &&
                bytes.copyOfRange(0, PNG_MAGIC.size).contentEquals(PNG_MAGIC)
        val isJpeg =
            bytes.size >= JPEG_MAGIC.size &&
                bytes.copyOfRange(0, JPEG_MAGIC.size).contentEquals(JPEG_MAGIC)
        return when {
            isPng -> MIME_TYPE_PNG
            isJpeg -> MIME_TYPE_JPEG
            else -> MIME_TYPE_PNG
        }
    }

    private fun normalizeBotUsername(
        sourceUsername: String,
        principal: String,
    ): String {
        val seed = principal.filter { it.isLetterOrDigit() }.ifBlank { "user123" }
        val base = sourceUsername.filter { it.isLetterOrDigit() }.ifBlank { seed }
        val trimmed = if (base.length > MAX_USERNAME_LENGTH) base.take(MAX_USERNAME_LENGTH) else base
        return if (trimmed.length >= MIN_USERNAME_LENGTH) {
            trimmed
        } else {
            (trimmed + seed).take(MIN_USERNAME_LENGTH)
        }
    }

    private fun buildRetryUsername(
        username: String,
        principal: String,
    ): String {
        val seed = principal.filter { it.isLetterOrDigit() }.ifBlank { "xyz123" }
        val suffix = seed.takeLast(RETRY_SUFFIX_LENGTH).padStart(RETRY_SUFFIX_LENGTH, '0')
        val prefixLength = (MAX_USERNAME_LENGTH - RETRY_SUFFIX_LENGTH).coerceAtLeast(0)
        val prefix = username.take(prefixLength)
        return normalizeBotUsername(sourceUsername = prefix + suffix, principal = principal)
    }

    private fun isUsernameTakenError(throwable: Throwable): Boolean = throwable is MetadataUpdateError.UsernameTaken

    private suspend fun downloadAvatar(url: String): ByteArray = httpClient.get(url).body()

    private suspend fun setActiveBotSession(
        botPrincipal: String,
        botIdentity: ByteArray,
        canisterData: com.yral.shared.rust.service.utils.CanisterData,
        profileDetails: AiInfluencerStep.ProfileDetails,
        profilePicUrl: String,
        displayUsername: String?,
    ) {
        val mainIdentitySnapshot = sessionManager.identity
        val mainPrincipalSnapshot = sessionManager.userPrincipal
        val resolvedUsername = resolveUsername(displayUsername, botPrincipal)
        // Switch in-memory session to bot for immediate use
        HelperService.initServiceFactories(botIdentity)
        val botSession =
            Session(
                identity = botIdentity,
                canisterId = canisterData.canisterId,
                userPrincipal = botPrincipal,
                profilePic = profilePicUrl,
                username = resolvedUsername,
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
        if (resolvedUsername != null) {
            preferences.putString(PrefKeys.USERNAME.name, resolvedUsername)
        } else {
            preferences.remove(PrefKeys.USERNAME.name)
        }
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

    private fun clearBotCreationProgress() {
        botCreationProgress = null
    }

    private data class UsernameUpdateResult(
        val username: String,
        val updated: Boolean,
    )

    private data class BotCreationProgress(
        val profileKey: String,
        var botIdentity: ByteArray? = null,
        var botPrincipal: String? = null,
        var registrationAccepted: Boolean = false,
        var canisterData: com.yral.shared.rust.service.utils.CanisterData? = null,
        var usernameResult: UsernameUpdateResult? = null,
        var avatarBytes: ByteArray? = null,
        var uploadedAvatarUrl: String? = null,
        var profileUpdated: Boolean = false,
        var createdInfluencer: com.yral.shared.features.aiinfluencer.domain.models.CreatedInfluencer? = null,
        var finalized: Boolean = false,
    )
}

class BotIdentityStorage(
    private val preferences: Preferences,
    private val json: Json,
    private val sessionManager: SessionManager,
) {
    suspend fun saveBotIdentity(
        principal: String,
        identity: ByteArray,
        username: String? = null,
    ) {
        val existing =
            preferences
                .getString(PrefKeys.BOT_IDENTITIES.name)
                ?.let { runCatching { json.decodeFromString<List<BotIdentityEntry>>(it) }.getOrNull() }
                ?: emptyList()
        val updated =
            existing
                .filterNot { it.principal == principal } +
                BotIdentityEntry(
                    principal = principal,
                    identity = identity.encodeBase64(),
                    username = username?.takeIf { it.isNotBlank() },
                )
        val encoded = json.encodeToString(updated)
        preferences.putString(PrefKeys.BOT_IDENTITIES.name, encoded)
        sessionManager.updateBotCount(updated.size)
    }

    @Serializable
    private data class BotIdentityEntry(
        val principal: String,
        val identity: String,
        val username: String? = null,
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

private fun AiInfluencerStep.ProfileDetails.progressKey(): String {
    val avatarKey = avatarBytes?.size?.toString() ?: avatarUrl
    return listOf(
        systemInstructions,
        name,
        displayName,
        description,
        avatarKey,
        initialGreeting,
        suggestedMessages.joinToString("|"),
        personalityTraits.entries.sortedBy { it.key }.joinToString("|") { "${it.key}:${it.value}" },
        category,
        isNsfw.toString(),
    ).joinToString("::")
}

data class AiInfluencerUiState(
    val step: AiInfluencerStep = AiInfluencerStep.DescriptionEntry(),
    val promptInput: String = "",
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
        val avatarBytes: ByteArray? = null,
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
        avatarBytes = null,
        initialGreeting = initialGreeting,
        suggestedMessages = suggestedMessages,
        personalityTraits = personalityTraits,
        isValid = isValid,
        validationReason = reason,
        category = category,
        isNsfw = isNsfw,
    )
