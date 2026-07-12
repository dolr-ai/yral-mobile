package com.yral.shared.features.profile.di

import com.yral.shared.core.di.CHAT_SERVER_BASE_URL
import com.yral.shared.features.profile.analytics.ProfileTelemetry
import com.yral.shared.features.profile.data.FollowersMetadataDataSourceImpl
import com.yral.shared.features.profile.data.ProfileDataSource
import com.yral.shared.features.profile.data.ProfileDataSourceImpl
import com.yral.shared.features.profile.data.ProfileRepositoryImpl
import com.yral.shared.features.profile.domain.DeleteVideoUseCase
import com.yral.shared.features.profile.domain.FollowNotificationUseCase
import com.yral.shared.features.profile.domain.UploadProfileImageUseCase
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.features.profile.videoideas.data.VideoIdeasDataSource
import com.yral.shared.features.profile.videoideas.data.VideoIdeasRemoteDataSource
import com.yral.shared.features.profile.videoideas.data.VideoIdeasRepositoryImpl
import com.yral.shared.features.profile.videoideas.domain.VideoIdeasRepository
import com.yral.shared.features.profile.videoideas.domain.usecases.GetVideoIdeasUseCase
import com.yral.shared.features.profile.videoideas.domain.usecases.MarkVideoIdeaUsedUseCase
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource
import com.yral.shared.rust.service.utils.CanisterData
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val profileModule =
    module {
        factoryOf(::FollowersMetadataDataSourceImpl).bind<FollowersMetadataDataSource>()
        factoryOf(::ProfileDataSourceImpl).bind<ProfileDataSource>()
        factoryOf(::ProfileRepositoryImpl).bind<ProfileRepository>()
        factoryOf(::DeleteVideoUseCase)
        factoryOf(::UploadProfileImageUseCase)
        factoryOf(::FollowNotificationUseCase)
        factoryOf(::ProfileTelemetry)
        // Video Ideas (Phase 22.3) — third profile tab, agent backend
        single<VideoIdeasDataSource> {
            VideoIdeasRemoteDataSource(
                httpClient = get(),
                json = get(),
                preferences = get(),
                chatBaseUrl = get(CHAT_SERVER_BASE_URL),
            )
        }
        factoryOf(::VideoIdeasRepositoryImpl) bind VideoIdeasRepository::class
        factoryOf(::GetVideoIdeasUseCase)
        factoryOf(::MarkVideoIdeaUsedUseCase)
        viewModel { parameters ->
            ProfileViewModel(
                canisterData = parameters.get<CanisterData>(),
                sessionManager = get(),
                profileRepository = get(),
                deleteVideoUseCase = get(),
                reportVideoUseCase = get(),
                followUserUseCase = get(),
                unfollowUserUseCase = get(),
                followNotificationUseCase = get(),
                getVideoViewsUseCase = get(),
                profileTelemetry = get(),
                chatTelemetry = get(),
                shareService = get(),
                urlBuilder = get(),
                linkGenerator = get(),
                crashlyticsManager = get(),
                flagManager = get(),
                userInfoPagingSourceFactory = get(),
                getUserProfileDetailsV7UseCase = get(),
                getInfluencerUseCase = get(),
                fileDownloader = get(),
                followersMetadataDataSource = get(),
                checkChatAccessUseCase = get(),
                createHumanConversationUseCase = get(),
                publishDraftVideoUseCase = get(),
                getVideoIdeasUseCase = get(),
                markVideoIdeaUsedUseCase = get(),
                getVideoProvidersUseCase = get(),
                generateVideoUseCase = get(),
                videoDraftPollingManager = get(),
            )
        }
        viewModelOf(::EditProfileViewModel)
    }
