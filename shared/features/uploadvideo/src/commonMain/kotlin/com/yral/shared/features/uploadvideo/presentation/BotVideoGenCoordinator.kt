package com.yral.shared.features.uploadvideo.presentation

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.uploadvideo.data.remote.models.TokenType
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.PollAndUploadAiVideoUseCase
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.ImageData
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal class BotVideoGenCoordinator(
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
    private val generateVideoUseCase: GenerateVideoUseCase,
    private val pollAndUploadAiVideoUseCase: PollAndUploadAiVideoUseCase,
    private val getProvidersUseCase: GetProvidersUseCase,
    private val crashlyticsManager: CrashlyticsManager,
    private val json: Json,
    appDispatchers: AppDispatchers,
) : BotVideoGenManager {
    private val logger = Logger.withTag("VideoGenRequest")
    private val scope = CoroutineScope(SupervisorJob() + appDispatchers.network)

    private var sessionJob: Job? = null
    private var pollingJob: Job? = null
    private var pollingPrincipal: String? = null

    init {
        start()
    }

    fun start() {
        if (sessionJob != null) return
        sessionJob =
            scope.launch {
                sessionManager.observeSessionState { it }.collect { state ->
                    handleSessionChange(state)
                }
            }
    }

    override fun enqueueGeneration(
        botPrincipal: String,
        prompt: String,
        imageData: ImageData,
    ) {
        scope.launch {
            if (!canEnqueue(botPrincipal, prompt)) return@launch
            val providerConfig =
                loadProviderConfig() ?: run {
                    logger.d { "bot_video_gen: no available provider, skipping generation for $botPrincipal" }
                    return@launch
                }
            val params = buildGenerateParams(botPrincipal, prompt, imageData, providerConfig)
            generateVideoUseCase(
                parameter = GenerateVideoUseCase.Param(params = params),
            ).onSuccess { result ->
                handleGenerateSuccess(
                    botPrincipal = botPrincipal,
                    prompt = prompt,
                    requestKey = result.requestKey,
                    modelName = providerConfig.modelName,
                )
            }.onFailure { error ->
                logger.e(error) { "bot_video_gen: generate failed for $botPrincipal" }
            }
        }
    }

    private suspend fun handleSessionChange(state: SessionState) {
        val signedIn = state as? SessionState.SignedIn
        val principal = signedIn?.session?.userPrincipal
        val isBot = signedIn?.session?.isBotAccount == true
        if (principal == null || !isBot) {
            cancelPolling()
        } else if (principal != pollingPrincipal || pollingJob?.isActive != true) {
            cancelPolling()
            val request = loadRequest(principal)
            if (request != null) {
                startPolling(
                    botPrincipal = principal,
                    requestKey =
                        VideoGenRequestKey(
                            counter = request.counter.toULong(),
                            principal = request.principal,
                        ),
                    modelName = request.modelName,
                    prompt = request.prompt,
                )
            }
        }
    }

    @Suppress("LongMethod")
    private fun startPolling(
        botPrincipal: String,
        requestKey: VideoGenRequestKey,
        modelName: String,
        prompt: String,
    ) {
        pollingPrincipal = botPrincipal
        pollingJob?.cancel()
        pollingJob =
            scope.launch {
                logger.d {
                    "bot_video_gen: polling started principal=${requestKey.principal} " +
                        "counter=${requestKey.counter}"
                }
                pollAndUploadAiVideoUseCase(
                    parameters =
                        PollAndUploadAiVideoUseCase.Params(
                            userPrincipal = botPrincipal,
                            modelName = modelName,
                            prompt = prompt,
                            requestKey = requestKey,
                            isFastInitially = false,
                            hashtags = emptyList(),
                            description = "",
                            isNsfw = false,
                            enableHotOrNot = false,
                            // Tell the polling use-case to skip the legacy
                            // client-side upload — server already created the draft.
                            uploadHandling = SERVER_DRAFT,
                        ),
                ).collect { result ->
                    result
                        .onSuccess { pollResult ->
                            when (pollResult) {
                                is PollAndUploadAiVideoUseCase.PollAndUploadResult.InProgress -> {
                                    logger.d { "bot_video_gen: polling in progress for $botPrincipal" }
                                }

                                is PollAndUploadAiVideoUseCase.PollAndUploadResult.Success -> {
                                    logger.d {
                                        "bot_video_gen: upload success for $botPrincipal " +
                                            "url=${pollResult.videoUrl}"
                                    }
                                    clearRequest(botPrincipal)
                                    cancelPolling()
                                }

                                is PollAndUploadAiVideoUseCase.PollAndUploadResult.Failed -> {
                                    crashlyticsManager.recordException(
                                        Exception(pollResult.errorMessage),
                                        ExceptionType.AI_VIDEO,
                                    )
                                    logger.w {
                                        "bot_video_gen: generation failed for $botPrincipal " +
                                            "reason=${pollResult.errorMessage}"
                                    }
                                    clearRequest(botPrincipal)
                                    cancelPolling()
                                }

                                is PollAndUploadAiVideoUseCase.PollAndUploadResult.UploadFailed -> {
                                    crashlyticsManager.recordException(
                                        Exception(pollResult.errorMessage),
                                        ExceptionType.AI_VIDEO,
                                    )
                                    logger.w {
                                        "bot_video_gen: upload failed for $botPrincipal " +
                                            "reason=${pollResult.errorMessage}"
                                    }
                                    cancelPolling()
                                }
                            }
                        }.onFailure { error ->
                            logger.e(error) { "bot_video_gen: polling failed for $botPrincipal" }
                        }
                }
            }
    }

    private fun cancelPolling() {
        pollingJob?.cancel()
        pollingJob = null
        pollingPrincipal = null
    }

    private suspend fun canEnqueue(
        botPrincipal: String,
        prompt: String,
    ): Boolean {
        val hasPrompt = prompt.isNotBlank()
        val hasRequest = loadRequest(botPrincipal) != null
        if (!hasPrompt) {
            logger.d { "bot_video_gen: skip empty prompt for $botPrincipal" }
        }
        if (hasRequest) {
            logger.d { "bot_video_gen: existing request for $botPrincipal, skipping" }
        }
        return hasPrompt && !hasRequest
    }

    private suspend fun loadProviderConfig(): ProviderConfig? {
        val provider = loadBotProvider() ?: return null
        val aspectRatio =
            when {
                provider.allowedAspectRatios.contains(PREFERRED_ASPECT_RATIO) -> PREFERRED_ASPECT_RATIO
                provider.defaultAspectRatio != null -> provider.defaultAspectRatio
                else -> provider.allowedAspectRatios.firstOrNull()
            }
        val durationSeconds = provider.defaultDuration ?: provider.allowedDurations.firstOrNull()
        return if (aspectRatio != null && durationSeconds != null) {
            ProviderConfig(
                providerId = provider.id,
                modelName = provider.name,
                aspectRatio = aspectRatio,
                durationSeconds = durationSeconds,
                resolution = provider.defaultResolution,
                generateAudio = if (provider.supportsAudio == true) true else null,
            )
        } else {
            null
        }
    }

    private fun buildGenerateParams(
        botPrincipal: String,
        prompt: String,
        imageData: ImageData,
        config: ProviderConfig,
    ) = GenerateVideoParams(
        providerId = config.providerId,
        prompt = prompt,
        aspectRatio = config.aspectRatio,
        resolution = config.resolution,
        durationSeconds = config.durationSeconds,
        generateAudio = config.generateAudio,
        image = imageData,
        tokenType = TokenType.FREE,
        userId = botPrincipal,
        // Server-side draft creation, matching AiVideoGenViewModel's manual flow.
        // Without this the legacy POST yral.com/api/upload_ai_video_from_url is
        // attempted after generation completes — that endpoint returns 405 since
        // the drafts migration (PR #932), causing welcome-video upload to fail.
        uploadHandling = SERVER_DRAFT,
    )

    private suspend fun handleGenerateSuccess(
        botPrincipal: String,
        prompt: String,
        requestKey: VideoGenRequestKey?,
        modelName: String,
    ) {
        if (requestKey == null) {
            logger.w { "bot_video_gen: missing request key for $botPrincipal" }
            return
        }
        logger.d {
            "bot_video_gen: requestKey received principal=${requestKey.principal} " +
                "counter=${requestKey.counter}"
        }
        saveRequest(
            botPrincipal = botPrincipal,
            request =
                BotVideoGenRequest(
                    principal = botPrincipal,
                    counter = requestKey.counter.toLong(),
                    modelName = modelName,
                    prompt = prompt,
                ),
        )
        if (sessionManager.userPrincipal == botPrincipal) {
            logger.d { "bot_video_gen: starting polling for $botPrincipal" }
            startPolling(
                botPrincipal = botPrincipal,
                requestKey = requestKey,
                modelName = modelName,
                prompt = prompt,
            )
        }
    }

    private suspend fun loadBotProvider(): Provider? {
        var provider: Provider? = null
        getProvidersUseCase()
            .onSuccess { list ->
                provider = list.firstOrNull { it.isAvailable != false }
            }.onFailure { error ->
                logger.e(error) { "bot_video_gen: failed to fetch providers" }
            }
        return provider
    }

    private suspend fun loadRequest(principal: String): BotVideoGenRequest? {
        val raw = preferences.getString(requestKey(principal)) ?: return null
        return runCatching { json.decodeFromString<BotVideoGenRequest>(raw) }.getOrNull()
    }

    private suspend fun saveRequest(
        botPrincipal: String,
        request: BotVideoGenRequest,
    ) {
        preferences.putString(requestKey(botPrincipal), json.encodeToString(request))
    }

    private suspend fun clearRequest(principal: String) {
        preferences.remove(requestKey(principal))
    }

    private fun requestKey(principal: String) = "${PrefKeys.BOT_VIDEO_GEN_REQUEST.name}_$principal"

    private data class ProviderConfig(
        val providerId: String,
        val modelName: String,
        val aspectRatio: String,
        val durationSeconds: Int,
        val resolution: String?,
        val generateAudio: Boolean?,
    )

    private companion object {
        private const val PREFERRED_ASPECT_RATIO = "9:16"

        // Mirrors AiVideoGenViewModel.SERVER_DRAFT — both flows now use the
        // server-handles-upload-and-draft pipeline introduced in PR #932.
        private const val SERVER_DRAFT = "ServerDraft"
    }
}
