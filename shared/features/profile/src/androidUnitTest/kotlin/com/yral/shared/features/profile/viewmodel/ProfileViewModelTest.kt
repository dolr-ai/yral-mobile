@file:OptIn(ExperimentalCoroutinesApi::class)
@file:Suppress("IllegalIdentifier")

package com.yral.shared.features.profile.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.videostate.VideoGenerationTracker
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.VideoViews
import com.yral.shared.data.domain.useCases.GetVideoViewsUseCase
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.data.ChatAccessBillingDataSource
import com.yral.shared.features.chat.data.models.ChatAccessApiResponse
import com.yral.shared.features.chat.data.models.GrantChatAccessRequestDto
import com.yral.shared.features.chat.data.models.GrantResult
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.domain.models.ConversationMessagesPageResult
import com.yral.shared.features.chat.domain.models.ConversationsPageResult
import com.yral.shared.features.chat.domain.models.DeleteConversationResult
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencersPageResult
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.features.chat.domain.usecases.CheckChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.features.profile.analytics.ProfileTelemetry
import com.yral.shared.features.profile.domain.DeleteVideoUseCase
import com.yral.shared.features.profile.domain.FollowNotificationUseCase
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.FollowNotification
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.features.uploadvideo.domain.PublishDraftVideoUseCase
import com.yral.shared.features.uploadvideo.domain.UploadRepository
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.domain.models.UploadAiVideoFromUrlRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadStatus
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
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
import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource
import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.pagedDataSource.UserInfoPagingSourceFactory
import com.yral.shared.rust.service.domain.usecases.FollowUserUseCase
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7UseCase
import com.yral.shared.rust.service.domain.usecases.UnfollowUserUseCase
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class ProfileViewModelTest {
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeUploadRepository: FakeUploadRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private var viewModel: ProfileViewModel? = null

    companion object {
        private const val TEST_PRINCIPAL = "test-principal"
        private const val TEST_CANISTER_ID = "test-canister"
        private const val TIMEOUT_MS = 5000L
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
        VideoGenerationTracker.stopGenerating()
        VideoGenerationTracker.consumeDraftsTabRequest()
    }

    @After
    fun tearDown() {
        viewModel?.viewModelScope?.cancel()
        viewModel = null
        Dispatchers.resetMain()
        VideoGenerationTracker.stopGenerating()
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
            commonApis = FakeCommonApis(),
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
                    FakeCommonApis(),
                    appDispatchers,
                    failureListener,
                ),
            profileTelemetry = ProfileTelemetry(AnalyticsManager()),
            chatTelemetry = ChatTelemetry(AnalyticsManager()),
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
            publishDraftVideoUseCase =
                PublishDraftVideoUseCase(
                    appDispatchers,
                    failureListener,
                    fakeUploadRepository,
                ),
        ).also { viewModel = it }
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

    // endregion
}

// region Fake implementations

private class NoOpUseCaseFailureListener : UseCaseFailureListener {
    override fun onFailure(
        throwable: Throwable,
        tag: String?,
        message: () -> String,
        exceptionType: String?,
    ) {
        // no-op
    }
}

private class FakeUploadRepository : UploadRepository {
    var markPostAsPublishedShouldThrow = false

    override suspend fun fetchUploadUrl(): UploadEndpoint = throw NotImplementedError()
    override fun uploadVideo(
        uploadUrl: String,
        filePath: String,
    ): Flow<UploadStatus> = emptyFlow()
    override suspend fun updateMetadata(uploadFileRequest: UploadFileRequest) = throw NotImplementedError()
    override suspend fun fetchProviders(): List<Provider> = throw NotImplementedError()
    override suspend fun generateVideo(params: GenerateVideoParams): GenerateVideoResult = throw NotImplementedError()
    override suspend fun uploadAiVideoFromUrl(request: UploadAiVideoFromUrlRequest): String = throw NotImplementedError()
    override suspend fun markPostAsPublished(postId: String) {
        if (markPostAsPublishedShouldThrow) throw YralException("Publish failed")
    }
}

private class FakeProfileRepository : ProfileRepository {
    override suspend fun getProfileVideos(
        canisterId: String,
        userPrincipal: String,
        isFromServiceCanister: Boolean,
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

    override suspend fun deleteVideo(request: DeleteVideoRequest) {}
    override suspend fun uploadProfileImage(imageBase64: String): String = ""
    override suspend fun followNotification(request: FollowNotification) {}
}

private class FakeCommonApis : CommonApis {
    override suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews> = emptyList()
    override suspend fun softDeleteInfluencer(
        principal: String,
        idToken: String,
        chatBaseUrl: String,
    ): Result<Unit> = Result.success(Unit)
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

private class FakeFollowersMetadataDataSource : FollowersMetadataDataSource {
    override suspend fun fetchUsernames(principals: List<String>): Map<String, String> = emptyMap()
}

private class FakeChatRepository : ChatRepository {
    override suspend fun getInfluencersPage(
        limit: Int,
        offset: Int,
    ): InfluencersPageResult = throw NotImplementedError()
    override suspend fun getTrendingInfluencersPage(
        limit: Int,
        offset: Int,
    ): InfluencersPageResult = throw NotImplementedError()
    override suspend fun getInfluencer(id: String): Influencer = throw NotImplementedError()
    override suspend fun createConversation(influencerId: String): Conversation = throw NotImplementedError()
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
    override suspend fun markConversationAsRead(conversationId: String) = throw NotImplementedError()
}

private class FakeChatAccessBillingDataSource : ChatAccessBillingDataSource {
    override val packageName: String = "com.yral.test"
    override suspend fun grantChatAccess(request: GrantChatAccessRequestDto): GrantResult = throw NotImplementedError()
    override suspend fun checkChatAccess(
        userId: String,
        botId: String,
    ): ChatAccessApiResponse = throw NotImplementedError()
}

// endregion
