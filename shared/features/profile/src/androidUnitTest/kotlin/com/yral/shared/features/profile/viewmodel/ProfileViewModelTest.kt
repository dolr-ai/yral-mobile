@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
@file:Suppress("IllegalIdentifier")

package com.yral.shared.features.profile.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.DeleteVideoInitiatedEventData
import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.analytics.events.VideoDeletedEventData
import com.yral.shared.analytics.events.VideoPublishedData
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.videostate.VideoGenerationTracker
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.DailyStreak
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.VideoViews
import com.yral.shared.data.domain.useCases.GetVideoViewsUseCase
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.data.ChatAccessBillingDataSource
import com.yral.shared.features.chat.data.models.ChatAccessApiResponse
import com.yral.shared.features.chat.data.models.GrantChatAccessRequestDto
import com.yral.shared.features.chat.data.models.GrantResult
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.Collage
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.domain.models.ConversationMessagesPageResult
import com.yral.shared.features.chat.domain.models.ConversationsPageResult
import com.yral.shared.features.chat.domain.models.DeleteConversationResult
import com.yral.shared.features.chat.domain.models.DiscoverySearchResult
import com.yral.shared.features.chat.domain.models.HumanCreatorTakeoverStatus
import com.yral.shared.features.chat.domain.models.InboxSearchResult
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencersPageResult
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.features.chat.domain.models.StreamEvent
import com.yral.shared.features.chat.domain.models.SystemPromptPreview
import com.yral.shared.features.chat.domain.usecases.CheckChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.CreateHumanConversationUseCase
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.features.profile.analytics.ProfileTelemetry
import com.yral.shared.features.profile.domain.DeleteVideoUseCase
import com.yral.shared.features.profile.domain.FollowNotificationUseCase
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.FollowNotification
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.features.profile.videoideas.domain.VideoIdeasRepository
import com.yral.shared.features.profile.videoideas.domain.models.VideoIdea
import com.yral.shared.features.profile.videoideas.domain.models.VideoIdeaStatus
import com.yral.shared.features.profile.videoideas.domain.usecases.GetVideoIdeasUseCase
import com.yral.shared.features.profile.videoideas.domain.usecases.MarkVideoIdeaUsedUseCase
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.PublishDraftVideoUseCase
import com.yral.shared.features.uploadvideo.domain.UploadRepository
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.features.uploadvideo.domain.models.InProgressDraft
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.domain.models.UploadAiVideoFromUrlRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadStatus
import com.yral.shared.features.uploadvideo.presentation.VideoDraftPollingManager
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.libs.filedownloader.FileDownloader
import com.yral.shared.libs.routing.deeplink.engine.RoutingTable
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.LinkInput
import com.yral.shared.libs.sharing.ShareService
import com.yral.shared.reportVideo.domain.IReportVideoRepository
import com.yral.shared.reportVideo.domain.ReportVideoUseCase
import com.yral.shared.reportVideo.domain.models.ReportRequest
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.pagedDataSource.UserInfoPagingSourceFactory
import com.yral.shared.rust.service.domain.usecases.FollowUserUseCase
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7UseCase
import com.yral.shared.rust.service.domain.usecases.UnfollowUserUseCase
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.testsupport.analytics.RecordingAnalyticsProvider
import com.yral.shared.testsupport.metadata.FakeFollowersMetadataDataSource
import com.yral.shared.testsupport.usecase.NoOpUseCaseFailureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@RunWith(RobolectricTestRunner::class)
class ProfileViewModelTest {
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeUploadRepository: FakeUploadRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeVideoIdeasRepository: FakeVideoIdeasRepository
    private lateinit var fakeAnalyticsProvider: RecordingAnalyticsProvider
    private lateinit var fakeCommonApis: FakeCommonApis
    private var viewModel: ProfileViewModel? = null

    companion object {
        private const val TEST_PRINCIPAL = "test-principal"
        private const val TEST_CANISTER_ID = "test-canister"
        private const val TIMEOUT_MS = 5000L
        private val TEST_VIDEO_IDEA =
            VideoIdea(
                id = "idea-1",
                influencerId = TEST_PRINCIPAL,
                batchDate = "2026-06-07",
                rank = 1,
                hook = "Hook",
                ideaText = "Create this video",
                status = VideoIdeaStatus.FRESH,
            )
        private val TEST_FEED_DETAILS =
            FeedDetails(
                postID = "post-1",
                videoID = "video-1",
                canisterID = TEST_CANISTER_ID,
                principalID = TEST_PRINCIPAL,
                url = "https://example.com/video.mp4",
                hashtags = emptyList(),
                thumbnail = "https://example.com/thumb.jpg",
                viewCount = 0u,
                displayName = "Test User",
                postDescription = "Test video",
                profileImageURL = null,
                likeCount = 0u,
                isLiked = false,
                nsfwProbability = null,
                isFollowing = false,
                isFromServiceCanister = false,
                userName = "testuser",
                isDraft = true,
            )

        private fun provider(
            id: String,
            isAvailable: Boolean = true,
            defaultAspectRatio: String? = "9:16",
            defaultDuration: Int? = 5,
        ) = Provider(
            id = id,
            name = id,
            description = null,
            cost = null,
            supportsImage = false,
            supportsNegativePrompt = false,
            supportsAudio = false,
            supportsSeed = false,
            allowedAspectRatios = listOf("9:16"),
            allowedResolutions = emptyList(),
            allowedDurations = listOf(5),
            defaultAspectRatio = defaultAspectRatio,
            defaultResolution = null,
            defaultDuration = defaultDuration,
            isAvailable = isAvailable,
            isInternal = false,
            modelIcon = null,
            extraInfo = null,
        )
    }

    private fun initComposeResourcesContext() {
        try {
            val ctx: Context = RuntimeEnvironment.getApplication()
            val clazz = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            val field = clazz.getDeclaredField("ANDROID_CONTEXT")
            field.isAccessible = true
            field.set(null, ctx)
        } catch (_: Exception) {
            // fallback: not critical for tests
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        initComposeResourcesContext()
        sessionManager = SessionManager()
        fakeUploadRepository = FakeUploadRepository()
        fakeProfileRepository = FakeProfileRepository()
        fakeVideoIdeasRepository = FakeVideoIdeasRepository()
        fakeAnalyticsProvider = RecordingAnalyticsProvider()
        fakeCommonApis = FakeCommonApis()
        VideoGenerationTracker.clearPendingGenerations()
        VideoGenerationTracker.consumeDraftsTabRequest()
    }

    @After
    fun tearDown() {
        viewModel?.viewModelScope?.cancel()
        viewModel = null
        Dispatchers.resetMain()
        VideoGenerationTracker.clearPendingGenerations()
        VideoGenerationTracker.consumeDraftsTabRequest()
    }

    private fun signInUser(principal: String = TEST_PRINCIPAL) {
        sessionManager.updateState(
            SessionState.SignedIn(Session(userPrincipal = principal)),
        )
    }

    private fun createViewModel(principal: String = TEST_PRINCIPAL): ProfileViewModel {
        val failureListener = NoOpUseCaseFailureListener()
        val appDispatchers = AppDispatchers()
        val fakeUserInfoRepository = FakeUserInfoRepository()
        val analyticsManager = AnalyticsManager(providers = listOf(fakeAnalyticsProvider))

        return ProfileViewModel(
            canisterData =
                CanisterData(
                    canisterId = TEST_CANISTER_ID,
                    userPrincipalId = principal,
                    profilePic = "https://example.com/pic.jpg",
                    username = "testuser",
                    isCreatedFromServiceCanister = false,
                ),
            sessionManager = sessionManager,
            profileRepository = fakeProfileRepository,
            deleteVideoUseCase =
                DeleteVideoUseCase(
                    fakeProfileRepository,
                    appDispatchers,
                    failureListener,
                ),
            reportVideoUseCase =
                ReportVideoUseCase(
                    FakeReportVideoRepository(),
                    sessionManager,
                    Json,
                    appDispatchers,
                    failureListener,
                ),
            followUserUseCase =
                FollowUserUseCase(
                    appDispatchers,
                    failureListener,
                    fakeUserInfoRepository,
                ),
            unfollowUserUseCase =
                UnfollowUserUseCase(
                    appDispatchers,
                    failureListener,
                    fakeUserInfoRepository,
                ),
            followNotificationUseCase =
                FollowNotificationUseCase(
                    sessionManager,
                    Json,
                    fakeProfileRepository,
                    appDispatchers,
                    failureListener,
                ),
            getVideoViewsUseCase =
                GetVideoViewsUseCase(
                    fakeCommonApis,
                    appDispatchers,
                    failureListener,
                ),
            profileTelemetry = ProfileTelemetry(analyticsManager),
            chatTelemetry = ChatTelemetry(analyticsManager),
            shareService = FakeShareService(),
            urlBuilder = UrlBuilder(RoutingTable(emptyList()), "https", "example.com"),
            linkGenerator = FakeLinkGenerator(),
            crashlyticsManager = CrashlyticsManager(),
            flagManager =
                FeatureFlagManager(
                    providersInPriority = emptyList(),
                    localProviderId = "test",
                ),
            userInfoPagingSourceFactory = UserInfoPagingSourceFactory(fakeUserInfoRepository),
            getUserProfileDetailsV7UseCase =
                GetUserProfileDetailsV7UseCase(
                    appDispatchers,
                    failureListener,
                    fakeUserInfoRepository,
                ),
            getInfluencerUseCase =
                GetInfluencerUseCase(
                    FakeChatRepository(),
                    appDispatchers,
                    failureListener,
                ),
            fileDownloader = FakeFileDownloader(),
            followersMetadataDataSource = FakeFollowersMetadataDataSource(),
            checkChatAccessUseCase =
                CheckChatAccessUseCase(
                    FakeChatAccessBillingDataSource(),
                    sessionManager,
                    appDispatchers,
                    failureListener,
                ),
            createHumanConversationUseCase =
                CreateHumanConversationUseCase(
                    FakeChatRepository(),
                    appDispatchers,
                    failureListener,
                ),
            publishDraftVideoUseCase =
                PublishDraftVideoUseCase(
                    appDispatchers,
                    failureListener,
                    fakeUploadRepository,
                ),
            getVideoIdeasUseCase =
                GetVideoIdeasUseCase(
                    fakeVideoIdeasRepository,
                    appDispatchers,
                    failureListener,
                ),
            markVideoIdeaUsedUseCase =
                MarkVideoIdeaUsedUseCase(
                    fakeVideoIdeasRepository,
                    appDispatchers,
                    failureListener,
                ),
            getVideoProvidersUseCase =
                GetProvidersUseCase(
                    appDispatchers,
                    failureListener,
                    fakeUploadRepository,
                ),
            generateVideoUseCase =
                GenerateVideoUseCase(
                    appDispatchers,
                    failureListener,
                    fakeUploadRepository,
                ),
            videoDraftPollingManager =
                VideoDraftPollingManager(
                    repository = fakeUploadRepository,
                    sessionManager = sessionManager,
                    appDispatchers = appDispatchers,
                    logger = YralLogger(),
                ),
        ).also { viewModel = it }
    }

    @Test
    fun `selecting Ideas loads once and caches the result`() =
        runBlocking {
            signInUser()
            fakeVideoIdeasRepository.ideas = listOf(TEST_VIDEO_IDEA)
            val vm = createViewModel()

            vm.selectTab(ProfileTab.Ideas)
            withTimeout(TIMEOUT_MS) {
                vm.state.first { it.videoIdeas == listOf(TEST_VIDEO_IDEA) }
            }
            vm.selectTab(ProfileTab.Published)
            vm.selectTab(ProfileTab.Ideas)

            assertEquals(1, fakeVideoIdeasRepository.listIdeasCalls)
        }

    @Test
    fun `create idea skips unavailable provider and uses allowed fallbacks`() =
        runBlocking {
            signInUser()
            fakeVideoIdeasRepository.ideas = listOf(TEST_VIDEO_IDEA)
            fakeUploadRepository.providers =
                listOf(
                    provider(id = "unavailable", isAvailable = false),
                    provider(
                        id = "usable",
                        isAvailable = true,
                        defaultAspectRatio = null,
                        defaultDuration = null,
                    ),
                )
            val vm = createViewModel()
            vm.selectTab(ProfileTab.Ideas)
            withTimeout(TIMEOUT_MS) {
                vm.state.first { it.videoIdeas != null }
            }

            vm.createVideoFromIdea(TEST_VIDEO_IDEA)
            withTimeout(TIMEOUT_MS) {
                while (fakeUploadRepository.generatedParams == null) delay(10)
            }

            val params = assertNotNull(fakeUploadRepository.generatedParams)
            assertEquals("usable", params.providerId)
            assertEquals("9:16", params.aspectRatio)
            assertEquals(5, params.durationSeconds)
        }

    @Test
    fun `provider error rolls back idea and releases generation lock`() =
        runBlocking {
            signInUser()
            fakeVideoIdeasRepository.ideas = listOf(TEST_VIDEO_IDEA)
            fakeUploadRepository.providers = listOf(provider(id = "provider"))
            fakeUploadRepository.generateResult =
                GenerateVideoResult(
                    operationId = null,
                    provider = "provider",
                    requestKey = null,
                    providerError = "Provider rejected request",
                )
            val vm = createViewModel()
            vm.selectTab(ProfileTab.Ideas)
            withTimeout(TIMEOUT_MS) {
                vm.state.first { it.videoIdeas != null }
            }

            vm.createVideoFromIdea(TEST_VIDEO_IDEA)
            withTimeout(TIMEOUT_MS) {
                vm.state.first { state ->
                    state.videoIdeas?.singleOrNull()?.status == VideoIdeaStatus.FRESH &&
                        fakeUploadRepository.generatedParams != null
                }
            }

            assertFalse(VideoGenerationTracker.state.value.isGenerating)
            assertEquals(0, fakeVideoIdeasRepository.markIdeaUsedCalls)
        }

    // region publishDraft success

    @Test
    fun `publishDraft success sets publishDraftUiState to Initial`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            vm.publishDraft(TEST_FEED_DETAILS)

            withTimeout(TIMEOUT_MS) {
                vm.state.first {
                    it.publishDraftUiState == UiState.Initial && it.selectedTab == ProfileTab.Published
                }
            }
            assertEquals(UiState.Initial, vm.state.value.publishDraftUiState)
        }

    @Test
    fun `publishDraft success switches to Published tab`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            vm.publishDraft(TEST_FEED_DETAILS)

            withTimeout(TIMEOUT_MS) {
                vm.state.first {
                    it.selectedTab == ProfileTab.Published && it.publishDraftUiState == UiState.Initial
                }
            }
            assertEquals(ProfileTab.Published, vm.state.value.selectedTab)
        }

    @Test
    fun `publishDraft success sends RefreshDrafts event`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            vm.publishDraft(TEST_FEED_DETAILS)

            val event = withTimeout(TIMEOUT_MS) { vm.profileEvents.first() }
            assertIs<ProfileEvents.RefreshDrafts>(event)
            Unit
        }

    @Test
    fun `publishDraft success tracks video published success event`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            vm.publishDraft(TEST_FEED_DETAILS)

            val event =
                awaitVideoPublishedEvent()
            assertEquals(TEST_FEED_DETAILS.videoID, event.videoId)
            assertEquals(true, event.isSuccess)
            assertEquals(null, event.reason)
        }

    @Test
    fun `publishDraft sends post id to publish use case`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            vm.publishDraft(TEST_FEED_DETAILS)

            withTimeout(TIMEOUT_MS) {
                vm.state.first {
                    it.publishDraftUiState == UiState.Initial && it.selectedTab == ProfileTab.Published
                }
            }
            assertEquals(listOf(TEST_FEED_DETAILS.postID), fakeUploadRepository.publishedPostIds)
        }

    // endregion

    // region publishDraft failure

    @Test
    fun `publishDraft failure sets publishDraftUiState to Failure`() =
        runBlocking {
            signInUser()
            fakeUploadRepository.markPostAsPublishedShouldThrow = true
            val vm = createViewModel()

            vm.publishDraft(TEST_FEED_DETAILS)

            withTimeout(TIMEOUT_MS) {
                vm.state.first { it.publishDraftUiState is UiState.Failure }
            }
            assertIs<UiState.Failure>(vm.state.value.publishDraftUiState)
            Unit
        }

    @Test
    fun `publishDraft failure tracks video published failure event`() =
        runBlocking {
            signInUser()
            fakeUploadRepository.markPostAsPublishedShouldThrow = true
            val vm = createViewModel()

            vm.publishDraft(TEST_FEED_DETAILS)

            val event =
                awaitVideoPublishedEvent()
            assertEquals(TEST_FEED_DETAILS.videoID, event.videoId)
            assertEquals(false, event.isSuccess)
            assertEquals(true, event.reason?.contains("Publish failed"))
        }

    // endregion

    // region video views

    @Test
    fun `showVideoViews uses existing total view count and fetches engaged views`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()
            val feedDetails =
                TEST_FEED_DETAILS.copy(
                    viewCount = 123u,
                    bulkViewCount = 456u,
                )
            fakeCommonApis.videoViews =
                listOf(
                    VideoViews(
                        videoId = feedDetails.videoID,
                        allViews = 999u,
                        loggedInViews = 42u,
                        lastFetched = Clock.System.now(),
                    ),
                )

            vm.showVideoViews(feedDetails)

            withTimeout(TIMEOUT_MS) {
                vm.state.first { state ->
                    (state.viewsData[feedDetails.videoID] as? UiState.Success)?.data?.loggedInViews == 42uL
                }
            }
            assertEquals(
                ProfileBottomSheet.VideoView(
                    videoId = feedDetails.videoID,
                    totalViews = 456u,
                ),
                vm.state.value.bottomSheet,
            )
            assertEquals(listOf(listOf(feedDetails.videoID)), fakeCommonApis.requestedVideoIds)
        }

    @Test
    fun `showVideoViews falls back to viewCount when bulkViewCount is absent`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()
            val feedDetails =
                TEST_FEED_DETAILS.copy(
                    viewCount = 123u,
                    bulkViewCount = null,
                )
            fakeCommonApis.videoViews =
                listOf(
                    VideoViews(
                        videoId = feedDetails.videoID,
                        allViews = 999u,
                        loggedInViews = 42u,
                        lastFetched = Clock.System.now(),
                    ),
                )

            vm.showVideoViews(feedDetails)

            withTimeout(TIMEOUT_MS) {
                vm.state.first { state -> state.viewsData[feedDetails.videoID] is UiState.Success }
            }
            assertEquals(
                ProfileBottomSheet.VideoView(
                    videoId = feedDetails.videoID,
                    totalViews = 123u,
                ),
                vm.state.value.bottomSheet,
            )
        }

    // endregion

    // region draft tab auto-selection

    @Test
    fun `VideoGenerationTracker selectDraftsTab switches to Drafts tab`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            VideoGenerationTracker.requestDraftsTab()

            withTimeout(TIMEOUT_MS) {
                vm.state.first { it.selectedTab == ProfileTab.Drafts }
            }
            assertEquals(ProfileTab.Drafts, vm.state.value.selectedTab)
        }

    @Test
    fun `VideoGenerationTracker targeted selectDraftsTab waits for matching profile`() =
        runBlocking {
            signInUser(principal = "bot-principal")
            val vm = createViewModel(principal = TEST_PRINCIPAL)

            VideoGenerationTracker.requestDraftsTab(targetPrincipal = "bot-principal")
            delay(50)

            assertEquals(ProfileTab.Published, vm.state.value.selectedTab)
            assertEquals("bot-principal", VideoGenerationTracker.selectDraftsTab.value?.targetPrincipal)
        }

    @Test
    fun `VideoGenerationTracker targeted selectDraftsTab switches matching profile to Drafts tab`() =
        runBlocking {
            signInUser(principal = "bot-principal")
            val vm = createViewModel(principal = "bot-principal")

            VideoGenerationTracker.requestDraftsTab(targetPrincipal = "bot-principal")

            withTimeout(TIMEOUT_MS) {
                vm.state.first { it.selectedTab == ProfileTab.Drafts }
            }
            assertEquals(ProfileTab.Drafts, vm.state.value.selectedTab)
        }

    @Test
    fun `draft refresh signal refreshes drafts without switching tabs`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            VideoGenerationTracker.startGenerating()
            VideoGenerationTracker.requestDraftsRefresh()

            val event = withTimeout(TIMEOUT_MS) { vm.profileEvents.first() }
            assertEquals(ProfileTab.Published, vm.state.value.selectedTab)
            assertIs<ProfileEvents.RefreshDrafts>(event)
            assertTrue(
                VideoGenerationTracker.state.value.pendingGenerations
                    .isNotEmpty(),
            )
        }

    // endregion

    // region delete draft

    @Test
    fun `deleteVideo for draft sends same request through delete use case`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            vm.confirmDelete(
                TEST_FEED_DETAILS,
                VideoDeleteCTA.PROFILE_THUMBNAIL,
            )
            vm.deleteVideo()

            withTimeout(TIMEOUT_MS) {
                vm.state.first { it.deleteConfirmation is DeleteConfirmationState.None }
            }
            assertEquals(1, fakeProfileRepository.deleteRequests.size)
            assertEquals(
                TEST_FEED_DETAILS.postID,
                fakeProfileRepository
                    .deleteRequests
                    .single()
                    .feedDetails
                    .postID,
            )
            assertEquals(
                TEST_FEED_DETAILS.videoID,
                fakeProfileRepository
                    .deleteRequests
                    .single()
                    .feedDetails
                    .videoID,
            )
            assertTrue(
                fakeProfileRepository
                    .deleteRequests
                    .single()
                    .feedDetails
                    .isDraft,
            )
        }

    @Test
    fun `deleteVideo failure for draft sets error state`() =
        runBlocking {
            signInUser()
            fakeProfileRepository.deleteVideoShouldThrow = true
            val vm = createViewModel()

            vm.confirmDelete(
                TEST_FEED_DETAILS,
                VideoDeleteCTA.PROFILE_THUMBNAIL,
            )
            vm.deleteVideo()

            withTimeout(TIMEOUT_MS) {
                vm.state.first { it.deleteConfirmation is DeleteConfirmationState.Error }
            }
            assertIs<DeleteConfirmationState.Error>(vm.state.value.deleteConfirmation)
            Unit
        }

    @Test
    fun `deleteVideo for draft tracks draft flag in delete telemetry`() =
        runBlocking {
            signInUser()
            val vm = createViewModel()

            vm.confirmDelete(
                TEST_FEED_DETAILS,
                VideoDeleteCTA.PROFILE_THUMBNAIL,
            )
            vm.deleteVideo()

            val initiatedEvent = awaitDeleteVideoInitiatedEvent()
            val deletedEvent = awaitVideoDeletedEvent()

            assertTrue(initiatedEvent.isDraft)
            assertTrue(deletedEvent.isDraft)
        }

    // endregion

    private suspend fun awaitVideoPublishedEvent(): VideoPublishedData {
        withTimeout(TIMEOUT_MS) {
            while (fakeAnalyticsProvider.events.filterIsInstance<VideoPublishedData>().isEmpty()) {
                delay(10)
            }
        }
        return fakeAnalyticsProvider.events.filterIsInstance<VideoPublishedData>().first()
    }

    private suspend fun awaitDeleteVideoInitiatedEvent(): DeleteVideoInitiatedEventData {
        withTimeout(TIMEOUT_MS) {
            while (fakeAnalyticsProvider.events.filterIsInstance<DeleteVideoInitiatedEventData>().isEmpty()) {
                delay(10)
            }
        }
        return fakeAnalyticsProvider.events.filterIsInstance<DeleteVideoInitiatedEventData>().first()
    }

    private suspend fun awaitVideoDeletedEvent(): VideoDeletedEventData {
        withTimeout(TIMEOUT_MS) {
            while (fakeAnalyticsProvider.events.filterIsInstance<VideoDeletedEventData>().isEmpty()) {
                delay(10)
            }
        }
        return fakeAnalyticsProvider.events.filterIsInstance<VideoDeletedEventData>().first()
    }
}

// region Fake implementations

private class FakeCommonApis : CommonApis {
    val requestedVideoIds = mutableListOf<List<String>>()
    var videoViews: List<VideoViews> = emptyList()

    override suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews> {
        requestedVideoIds += videoId
        return videoViews
    }

    override suspend fun softDeleteInfluencer(
        principal: String,
        idToken: String,
        chatBaseUrl: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun fetchDailyStreak(
        userPrincipal: String,
        idToken: String,
    ): DailyStreak = throw NotImplementedError()
}

private class FakeUploadRepository : UploadRepository {
    var markPostAsPublishedShouldThrow = false
    val publishedPostIds = mutableListOf<String>()
    var providers: List<Provider> = emptyList()
    var generateResult =
        GenerateVideoResult(
            operationId = "operation-1",
            provider = "provider",
            requestKey = null,
            providerError = null,
        )
    var generatedParams: GenerateVideoParams? = null

    override suspend fun fetchUploadUrl(): UploadEndpoint = throw NotImplementedError()
    override fun uploadVideo(
        uploadUrl: String,
        filePath: String,
    ): Flow<UploadStatus> = emptyFlow()
    override suspend fun updateMetadata(uploadFileRequest: UploadFileRequest) = throw NotImplementedError()
    override suspend fun fetchProviders(): List<Provider> = providers
    override suspend fun generateVideo(params: GenerateVideoParams): GenerateVideoResult {
        generatedParams = params
        return generateResult
    }
    override suspend fun getInProgressDrafts(userId: String): List<InProgressDraft> = emptyList()
    override suspend fun uploadAiVideoFromUrl(request: UploadAiVideoFromUrlRequest): String = throw NotImplementedError()
    override suspend fun markPostAsPublished(postId: String) {
        publishedPostIds += postId
        if (markPostAsPublishedShouldThrow) throw YralException("Publish failed")
    }
}

private class FakeVideoIdeasRepository : VideoIdeasRepository {
    var ideas: List<VideoIdea> = emptyList()
    var listIdeasCalls = 0
    var markIdeaUsedCalls = 0

    override suspend fun listIdeas(influencerId: String): List<VideoIdea> {
        listIdeasCalls += 1
        return ideas
    }

    override suspend fun markIdeaUsed(
        influencerId: String,
        ideaId: String,
    ): VideoIdea {
        markIdeaUsedCalls += 1
        return ideas.first { it.id == ideaId }.copy(status = VideoIdeaStatus.USED)
    }
}

private class FakeProfileRepository : ProfileRepository {
    val deleteRequests = mutableListOf<DeleteVideoRequest>()
    var deleteVideoShouldThrow = false

    override suspend fun getProfileVideos(
        canisterId: String,
        userPrincipal: String,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult =
        ProfileVideosPageResult(
            posts = emptyList(),
            hasNextPage = false,
            nextStartIndex = 0u,
        )

    override suspend fun getDraftVideos(
        canisterId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult =
        ProfileVideosPageResult(
            posts = emptyList(),
            hasNextPage = false,
            nextStartIndex = 0u,
        )

    override suspend fun deleteVideo(request: DeleteVideoRequest) {
        deleteRequests += request
        if (deleteVideoShouldThrow) throw YralException("Delete failed")
    }
    override suspend fun uploadProfileImage(imageBase64: String): String = ""
    override suspend fun followNotification(request: FollowNotification) {}
}

private class FakeReportVideoRepository : IReportVideoRepository {
    override suspend fun reportVideo(request: ReportRequest): String = ""
}

private class FakeUserInfoRepository : UserInfoRepository {
    override suspend fun followUser(
        principal: String,
        targetPrincipal: String,
    ) = throw NotImplementedError()
    override suspend fun unfollowUser(
        principal: String,
        targetPrincipal: String,
    ) = throw NotImplementedError()
    override suspend fun getUserProfileDetailsV7(
        principal: String,
        targetPrincipal: String,
    ): UserProfileDetails = throw NotImplementedError()
    override suspend fun getUsersProfileDetails(
        principal: String,
        targetPrincipalIds: List<String>,
    ): Map<String, UserProfileDetails> = throw NotImplementedError()
    override suspend fun getFollowers(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowersPageResult = throw NotImplementedError()
    override suspend fun getFollowing(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowingPageResult = throw NotImplementedError()
    override suspend fun updateProfileDetailsV2(
        principal: String,
        details: ProfileUpdateDetailsV2,
    ) = throw NotImplementedError()
    override suspend fun acceptNewUserRegistrationV2(
        principal: String,
        newPrincipal: String,
        authenticated: Boolean,
        mainAccount: String?,
    ) = throw NotImplementedError()
}

private class FakeShareService : ShareService {
    override suspend fun shareImageWithText(
        imageUrl: String,
        text: String,
    ) {}
    override suspend fun shareText(text: String) {}
}

private class FakeLinkGenerator : LinkGenerator {
    override suspend fun generateShareLink(input: LinkInput): String = "https://example.com/share"
}

private class FakeFileDownloader : FileDownloader {
    override suspend fun downloadFile(
        url: String,
        fileName: String,
        saveToGallery: Boolean,
    ): com.github.michaelbull.result.Result<String, YralException> = Err(YralException("not implemented"))
}

private class FakeChatRepository : ChatRepository {
    override suspend fun getUnreadConversationCount(principal: String): Int = throw NotImplementedError()

    override suspend fun getInfluencersPage(
        limit: Int,
        offset: Int,
    ): InfluencersPageResult = throw NotImplementedError()
    override suspend fun getTrendingInfluencersPage(
        limit: Int,
        offset: Int,
    ): InfluencersPageResult = throw NotImplementedError()
    override suspend fun getInfluencer(id: String): Influencer = throw NotImplementedError()
    override suspend fun requestInfluencerImages(
        influencerId: String,
        isSubscribed: Boolean,
    ): Collage = throw NotImplementedError()
    override suspend fun getInfluencerCollage(
        influencerId: String,
        isSubscribed: Boolean,
        collageId: String?,
        date: String?,
    ): Collage = throw NotImplementedError()
    override suspend fun searchDiscovery(
        query: String,
        limit: Int,
    ): List<DiscoverySearchResult> = throw NotImplementedError()
    override suspend fun searchInbox(
        query: String,
        limit: Int,
    ): List<InboxSearchResult> = throw NotImplementedError()
    override suspend fun getSystemPromptPreview(botId: String): SystemPromptPreview = throw NotImplementedError()
    override suspend fun createConversation(influencerId: String): Conversation = throw NotImplementedError()
    override suspend fun createHumanConversation(participantId: String): Conversation = throw NotImplementedError()
    override suspend fun sendHumanMessage(
        conversationId: String,
        draft: SendMessageDraft,
    ): SendMessageResult = throw NotImplementedError()
    override suspend fun getConversationsPage(
        limit: Int,
        offset: Int,
        influencerId: String?,
        principal: String,
    ): ConversationsPageResult = throw NotImplementedError()
    override suspend fun deleteConversation(conversationId: String): DeleteConversationResult = throw NotImplementedError()
    override suspend fun getConversationMessagesPage(
        conversationId: String,
        limit: Int,
        offset: Int,
    ): ConversationMessagesPageResult = throw NotImplementedError()
    override suspend fun sendMessage(
        conversationId: String,
        draft: SendMessageDraft,
    ): SendMessageResult = throw NotImplementedError()
    override fun streamMessage(
        conversationId: String,
        draft: SendMessageDraft,
    ): Flow<StreamEvent> = emptyFlow()
    override suspend fun markConversationAsRead(conversationId: String) = throw NotImplementedError()
    override suspend fun startHumanCreatorTakeover(conversationId: String): HumanCreatorTakeoverStatus = throw NotImplementedError()
    override suspend fun releaseHumanCreatorTakeover(conversationId: String) = throw NotImplementedError()
    override suspend fun sendHumanCreatorMessage(
        conversationId: String,
        content: String,
    ): ChatMessage = throw NotImplementedError()
    override suspend fun getHumanCreatorTakeoverStatus(conversationId: String): HumanCreatorTakeoverStatus = throw NotImplementedError()
    override suspend fun getCreatorConversationMessagesPage(
        conversationId: String,
        limit: Int,
        offset: Int,
    ): ConversationMessagesPageResult = throw NotImplementedError()
}

private class FakeChatAccessBillingDataSource : ChatAccessBillingDataSource {
    override val packageName: String = "com.yral.test"
    override suspend fun grantChatAccess(request: GrantChatAccessRequestDto): GrantResult = throw NotImplementedError()
    override suspend fun grantBotSubscription(request: GrantChatAccessRequestDto): GrantResult = throw NotImplementedError()
    override suspend fun checkChatAccess(
        userId: String,
        botId: String,
    ): ChatAccessApiResponse = throw NotImplementedError()
}

// endregion
