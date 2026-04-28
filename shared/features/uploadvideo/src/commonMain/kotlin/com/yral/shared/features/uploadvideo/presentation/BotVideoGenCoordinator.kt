package com.yral.shared.features.uploadvideo.presentation

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.videostate.VideoGenerationTracker
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.uploadvideo.data.remote.models.TokenType
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.ImageData
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class BotVideoGenCoordinator(
    private val generateVideoUseCase: GenerateVideoUseCase,
    private val getProvidersUseCase: GetProvidersUseCase,
    private val crashlyticsManager: CrashlyticsManager,
    appDispatchers: AppDispatchers,
) : BotVideoGenManager {
    private val logger = Logger.withTag("VideoGenRequest")
    private val scope = CoroutineScope(SupervisorJob() + appDispatchers.network)

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
                result.providerError?.let { error ->
                    crashlyticsManager.recordException(Exception(error), ExceptionType.AI_VIDEO)
                    logger.w { "bot_video_gen: generate provider error for $botPrincipal reason=$error" }
                    return@onSuccess
                }
                VideoGenerationTracker.startGenerating()
                logger.d {
                    "bot_video_gen: server draft generation submitted for $botPrincipal " +
                        "requestKey=${result.requestKey}"
                }
            }.onFailure { error ->
                crashlyticsManager.recordException(
                    error as? Exception ?: Exception(error),
                    ExceptionType.AI_VIDEO,
                )
                logger.e(error) { "bot_video_gen: generate failed for $botPrincipal" }
            }
        }
    }

    private fun canEnqueue(
        botPrincipal: String,
        prompt: String,
    ): Boolean {
        val hasPrompt = prompt.isNotBlank()
        if (!hasPrompt) {
            logger.d { "bot_video_gen: skip empty prompt for $botPrincipal" }
        }
        return hasPrompt
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
