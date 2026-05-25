@file:OptIn(ExperimentalCoroutinesApi::class)

package com.yral.shared.features.uploadvideo.presentation

import com.russhwolf.settings.MapSettings
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.videostate.VideoGenerationTracker
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.CrashlyticsProvider
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.subscriptions.analytics.SubscriptionTelemetry
import com.yral.shared.features.uploadvideo.analytics.UploadVideoTelemetry
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetFreeCreditsStatusUseCase
import com.yral.shared.features.uploadvideo.domain.GetPropertyRateLimitConfigUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.UploadRepository
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoErrorType
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.features.uploadvideo.domain.models.InProgressDraft
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.domain.models.ProviderCost
import com.yral.shared.features.uploadvideo.domain.models.UploadAiVideoFromUrlRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadStatus
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.stores.AffiliateAttributionStore
import com.yral.shared.rust.service.domain.RateLimitRepository
import com.yral.shared.rust.service.domain.models.PropertyRateLimitConfig
import com.yral.shared.rust.service.domain.models.RateLimitStatus
import com.yral.shared.rust.service.domain.models.Result2
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey
import com.yral.shared.testsupport.preferences.FakePreferences
import com.yral.shared.testsupport.usecase.NoOpUseCaseFailureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiVideoGenViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeUploadRepository: FakeUploadRepository
    private lateinit var fakeRateLimitRepository: FakeRateLimitRepository
    private lateinit var fakePreferences: FakePreferences
    private lateinit var fakeCrashlyticsProvider: FakeCrashlyticsProvider

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionManager = SessionManager()
        fakeUploadRepository = FakeUploadRepository()
        fakeRateLimitRepository = FakeRateLimitRepository()
        fakePreferences = FakePreferences()
        fakeCrashlyticsProvider = FakeCrashlyticsProvider()
        VideoGenerationTracker.clearPendingGenerations()
        VideoGenerationTracker.consumeDraftsTabRequest()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        VideoGenerationTracker.clearPendingGenerations()
    }

    private fun createViewModel(): AiVideoGenViewModel {
        val failureListener = NoOpUseCaseFailureListener()
        val appDispatchers = AppDispatchers()
        val uploadVideoTelemetry =
            UploadVideoTelemetry(
                analyticsManager = AnalyticsManager(),
                sessionManager = sessionManager,
                affiliateAttributionStore = AffiliateAttributionStore(MapSettings()),
            )
        return AiVideoGenViewModel(
            requiredUseCases =
                AiVideoGenViewModel.RequiredUseCases(
                    getProviders = GetProvidersUseCase(appDispatchers, failureListener, fakeUploadRepository),
                    getFreeCreditsStatus =
                        GetFreeCreditsStatusUseCase(appDispatchers, failureListener, fakeRateLimitRepository),
                    getPropertyRateLimitConfig =
                        GetPropertyRateLimitConfigUseCase(appDispatchers, failureListener, fakeRateLimitRepository),
                    generateVideo = GenerateVideoUseCase(appDispatchers, failureListener, fakeUploadRepository),
                ),
            sessionManager = sessionManager,
            preferences = fakePreferences,
            uploadVideoTelemetry = uploadVideoTelemetry,
            subscriptionTelemetry = SubscriptionTelemetry(AnalyticsManager()),
            videoDraftPollingManager =
                VideoDraftPollingManager(
                    repository = fakeUploadRepository,
                    sessionManager = sessionManager,
                    appDispatchers = appDispatchers,
                    logger = YralLogger(),
                ),
            crashlyticsManager = CrashlyticsManager(listOf(fakeCrashlyticsProvider)),
            logger = YralLogger(),
            flagManager = FeatureFlagManager(providersInPriority = emptyList(), localProviderId = "test"),
        )
    }

    private fun signInUser(userPrincipal: String = "test-principal") {
        sessionManager.updateState(
            SessionState.SignedIn(Session(userPrincipal = userPrincipal)),
        )
    }

    private suspend fun setupProviderAndPrompt(viewModel: AiVideoGenViewModel) {
        viewModel.refresh("test-canister")
        viewModel.state.first { it.selectedProvider != null }
        viewModel.updatePromptText("A beautiful sunset over the ocean")
    }

    // region 1: generateAiVideo sets UiState to InProgress

    @Test
    fun `generateAiVideo sets uiState to InProgress`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoSuspend = true
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()

            val state = viewModel.state.value
            assertIs<UiState.InProgress>(state.uiState)
        }

    @Test
    fun `refresh auto-selects ltx provider returned by providers api`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.refresh("test-canister")
            val state = viewModel.state.first { it.selectedProvider != null }

            assertEquals("ltx", state.selectedProvider?.id)
        }

    @Test
    fun `generateAiVideo starts VideoGenerationTracker`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoSuspend = true
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()

            assertTrue(VideoGenerationTracker.state.value.isGenerating)
        }

    // endregion

    @Test
    fun `in progress draft sync does not request drafts refresh when count is unchanged`() =
        runTest {
            VideoGenerationTracker.startGenerating()
            fakeUploadRepository.inProgressDrafts = listOf(inProgressDraft("op-1"))
            var refreshCount = 0
            val refreshJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    VideoGenerationTracker.refreshDrafts.collect {
                        refreshCount++
                    }
                }
            val manager =
                VideoDraftPollingManager(
                    repository = fakeUploadRepository,
                    sessionManager = sessionManager,
                    appDispatchers = AppDispatchers(),
                    logger = YralLogger(),
                )

            manager.syncInProgressDrafts("test-principal")

            assertEquals(1, VideoGenerationTracker.state.value.pendingGenerations.size)
            assertFalse(VideoGenerationTracker.state.value.isDraftRefreshPending)
            assertEquals(0, refreshCount)
            refreshJob.cancel()
        }

    @Test
    fun `in progress draft sync does not request drafts refresh when pending count increases`() =
        runTest {
            fakeUploadRepository.inProgressDrafts = listOf(inProgressDraft("op-1"))
            var refreshCount = 0
            val refreshJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    VideoGenerationTracker.refreshDrafts.collect {
                        refreshCount++
                    }
                }
            val manager =
                VideoDraftPollingManager(
                    repository = fakeUploadRepository,
                    sessionManager = sessionManager,
                    appDispatchers = AppDispatchers(),
                    logger = YralLogger(),
                )

            manager.syncInProgressDrafts("test-principal")

            assertEquals(1, VideoGenerationTracker.state.value.pendingGenerations.size)
            assertFalse(VideoGenerationTracker.state.value.isDraftRefreshPending)
            assertEquals(0, refreshCount)
            refreshJob.cancel()
        }

    @Test
    fun `in progress draft sync requests drafts refresh when pending count decreases`() =
        runTest {
            VideoGenerationTracker.startGenerating()
            fakeUploadRepository.inProgressDrafts = emptyList()
            var refreshCount = 0
            val refreshJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    VideoGenerationTracker.refreshDrafts.collect {
                        refreshCount++
                    }
                }
            val manager =
                VideoDraftPollingManager(
                    repository = fakeUploadRepository,
                    sessionManager = sessionManager,
                    appDispatchers = AppDispatchers(),
                    logger = YralLogger(),
                )

            manager.syncInProgressDrafts("test-principal")

            assertFalse(VideoGenerationTracker.state.value.isGenerating)
            assertTrue(VideoGenerationTracker.state.value.isDraftRefreshPending)
            assertEquals(1, refreshCount)
            refreshJob.cancel()
        }

    // region 2: successful generation resets uiState to Initial

    @Test
    fun `successful generation resets uiState to Initial`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoResult = SUCCESS_RESULT
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            // Wait for the state to settle (use case runs on Dispatchers.IO then resumes on Main)
            viewModel.state.first { it.uiState == UiState.Initial }

            assertEquals(UiState.Initial, viewModel.state.value.uiState)
        }

    @Test
    fun `successful generation keeps VideoGenerationTracker active until draft is created`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoResult = SUCCESS_RESULT
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            viewModel.state.first { it.uiState == UiState.Initial }

            assertTrue(VideoGenerationTracker.state.value.isGenerating)
        }

    // endregion

    // region 3: successful generation sends both toast and navigation events

    @Test
    fun `successful generation sends toast and navigation events in order`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoResult = SUCCESS_RESULT
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            // Wait for the state to settle (generation complete)
            viewModel.state.first { it.uiState == UiState.Initial }

            val events = viewModel.aiVideoGenEvents.take(2).toList()

            assertIs<AiVideoGenViewModel.AiVideoGenEvent.ShowGeneratedToast>(events[0])
            assertIs<AiVideoGenViewModel.AiVideoGenEvent.NavigateToHome>(events[1])
        }

    // endregion

    // region 4: successful generation does NOT call requestDraftsTab

    @Test
    fun `successful generation does not request drafts tab`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoResult = SUCCESS_RESULT
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            viewModel.state.first { it.uiState == UiState.Initial }

            assertNull(VideoGenerationTracker.selectDraftsTab.value)
        }

    // endregion

    // region 5: provider error shows error bottom sheet

    @Test
    fun `provider error shows error bottom sheet`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoResult = PROVIDER_ERROR_RESULT
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            val state =
                viewModel.state.first { it.bottomSheetType is AiVideoGenViewModel.BottomSheetType.Error }

            val bottomSheet = state.bottomSheetType
            assertIs<AiVideoGenViewModel.BottomSheetType.Error>(bottomSheet)
            assertEquals(GenerateVideoErrorType.PROVIDER_ERROR, bottomSheet.title)
            assertEquals("Content policy violation", bottomSheet.message)
            assertTrue(bottomSheet.endFlow)
            assertEquals(UiState.Initial, state.uiState)
        }

    @Test
    fun `provider error stops VideoGenerationTracker`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoResult = PROVIDER_ERROR_RESULT
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            viewModel.state.first { it.bottomSheetType is AiVideoGenViewModel.BottomSheetType.Error }

            assertFalse(VideoGenerationTracker.state.value.isGenerating)
        }

    @Test
    fun `provider error is recorded to Crashlytics as AI video exception`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoResult = PROVIDER_ERROR_RESULT
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            viewModel.state.first { it.bottomSheetType is AiVideoGenViewModel.BottomSheetType.Error }

            assertEquals(listOf(ExceptionType.AI_VIDEO), fakeCrashlyticsProvider.recordedTypes)
        }

    @Test
    fun `tryAgain after provider error retries with same prompt and provider`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoResults += PROVIDER_ERROR_RESULT
            fakeUploadRepository.generateVideoResults += SUCCESS_RESULT
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            viewModel.state.first { it.bottomSheetType is AiVideoGenViewModel.BottomSheetType.Error }

            viewModel.tryAgain()
            viewModel.state.first { it.uiState == UiState.Initial }

            assertEquals(2, fakeUploadRepository.generateVideoParams.size)
            assertEquals(TEST_PROVIDER.id, fakeUploadRepository.generateVideoParams[0].providerId)
            assertEquals(TEST_PROVIDER.id, fakeUploadRepository.generateVideoParams[1].providerId)
            assertEquals("A beautiful sunset over the ocean", fakeUploadRepository.generateVideoParams[0].prompt)
            assertEquals("A beautiful sunset over the ocean", fakeUploadRepository.generateVideoParams[1].prompt)
        }

    // endregion

    // region 6: API failure handling

    @Test
    fun `api failure shows error bottom sheet`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoShouldThrow = true
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            val state =
                viewModel.state.first { it.bottomSheetType is AiVideoGenViewModel.BottomSheetType.Error }

            val bottomSheet = state.bottomSheetType
            assertIs<AiVideoGenViewModel.BottomSheetType.Error>(bottomSheet)
            assertTrue(bottomSheet.endFlow)
            assertEquals(UiState.Initial, state.uiState)
        }

    @Test
    fun `api failure stops VideoGenerationTracker`() =
        runTest {
            signInUser()
            fakeUploadRepository.generateVideoShouldThrow = true
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()
            viewModel.state.first { it.bottomSheetType is AiVideoGenViewModel.BottomSheetType.Error }

            assertFalse(VideoGenerationTracker.state.value.isGenerating)
        }

    // endregion

    // region 7: no userPrincipal / no provider skips generation

    @Test
    fun `generateAiVideo does nothing when user is not signed in`() =
        runTest {
            val viewModel = createViewModel()
            setupProviderAndPrompt(viewModel)

            viewModel.generateAiVideo()

            assertEquals(UiState.Initial, viewModel.state.value.uiState)
            assertFalse(VideoGenerationTracker.state.value.isGenerating)
        }

    @Test
    fun `generateAiVideo does nothing when no provider selected`() =
        runTest {
            signInUser()
            val viewModel = createViewModel()
            viewModel.updatePromptText("Test prompt")

            viewModel.generateAiVideo()

            assertEquals(UiState.Initial, viewModel.state.value.uiState)
        }

    // endregion

    companion object {
        val TEST_PROVIDER =
            Provider(
                id = "ltx",
                name = "LTX",
                description = "LTX video generation",
                cost = ProviderCost(usdCents = null, dolr = null, sats = 100),
                supportsImage = false,
                supportsNegativePrompt = false,
                supportsAudio = false,
                supportsSeed = false,
                allowedAspectRatios = listOf("16:9"),
                allowedResolutions = listOf("720p"),
                allowedDurations = listOf(5),
                defaultAspectRatio = "16:9",
                defaultResolution = "720p",
                defaultDuration = 5,
                isAvailable = true,
                isInternal = false,
                modelIcon = null,
                extraInfo = null,
            )

        private val SUCCESS_RESULT =
            GenerateVideoResult(
                operationId = "op-1",
                provider = "test-provider",
                requestKey = null,
                providerError = null,
            )

        private val PROVIDER_ERROR_RESULT =
            GenerateVideoResult(
                operationId = "op-1",
                provider = "test-provider",
                requestKey = null,
                providerError = "Content policy violation",
                errorType = GenerateVideoErrorType.PROVIDER_ERROR,
            )

        private fun inProgressDraft(operationId: String) =
            InProgressDraft(
                createdAt = "2026-05-18T00:00:00Z",
                modelId = "model-1",
                operationId = operationId,
                prompt = "prompt",
                provider = "provider",
                status = "in_progress",
                thumbnailUrl = null,
            )
    }
}

// region fakes

internal class FakeUploadRepository : UploadRepository {
    var providers: List<Provider> = listOf(AiVideoGenViewModelTest.TEST_PROVIDER)
    var generateVideoResult: GenerateVideoResult? = null
    val generateVideoResults = mutableListOf<GenerateVideoResult>()
    val generateVideoParams = mutableListOf<GenerateVideoParams>()
    var generateVideoShouldThrow = false
    var generateVideoSuspend = false
    var inProgressDrafts: List<InProgressDraft> = emptyList()

    override suspend fun fetchUploadUrl(): UploadEndpoint = throw NotImplementedError()

    override fun uploadVideo(
        uploadUrl: String,
        filePath: String,
    ): Flow<UploadStatus> = emptyFlow()

    override suspend fun updateMetadata(uploadFileRequest: UploadFileRequest) {}

    override suspend fun fetchProviders(): List<Provider> = providers

    override suspend fun generateVideo(params: GenerateVideoParams): GenerateVideoResult {
        generateVideoParams += params
        if (generateVideoSuspend) kotlinx.coroutines.awaitCancellation()
        if (generateVideoShouldThrow) throw RuntimeException("Network error")
        if (generateVideoResults.isNotEmpty()) return generateVideoResults.removeAt(0)
        return generateVideoResult ?: throw IllegalStateException("No result configured")
    }

    override suspend fun getInProgressDrafts(userId: String): List<InProgressDraft> = inProgressDrafts

    override suspend fun uploadAiVideoFromUrl(request: UploadAiVideoFromUrlRequest): String = "video-id"

    override suspend fun markPostAsPublished(postId: String) {}
}

internal class FakeRateLimitRepository : RateLimitRepository {
    override suspend fun fetchVideoGenerationStatus(
        userPrincipal: String,
        requestKey: VideoGenRequestKey,
    ): Result2 = Result2.Err("Not configured")

    override suspend fun getVideoGenFreeCreditsStatus(
        userPrincipal: String,
        isRegistered: Boolean,
    ): RateLimitStatus? = null

    override suspend fun getPropertyRateLimitConfig(
        userPrincipal: String,
        property: String,
    ): PropertyRateLimitConfig? = null
}

internal class FakeCrashlyticsProvider : CrashlyticsProvider {
    override val name: String = "fake"
    val recordedTypes = mutableListOf<ExceptionType>()

    override fun recordException(exception: Exception) = Unit

    override fun recordException(
        exception: Exception,
        type: ExceptionType,
    ) {
        recordedTypes += type
    }

    override fun logMessage(message: String) = Unit

    override fun setUserId(id: String) = Unit
}

// endregion
