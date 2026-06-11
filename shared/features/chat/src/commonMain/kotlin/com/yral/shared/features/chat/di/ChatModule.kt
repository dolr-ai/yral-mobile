package com.yral.shared.features.chat.di

import com.yral.shared.core.di.CHAT_SERVER_BASE_URL
import com.yral.shared.core.di.INFLUENCER_FEED_SERVER_BASE_URL
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.data.ChatAccessBillingDataSource
import com.yral.shared.features.chat.data.ChatAccessBillingRemoteDataSource
import com.yral.shared.features.chat.data.ChatDataSource
import com.yral.shared.features.chat.data.ChatRemoteDataSource
import com.yral.shared.features.chat.data.ChatRepositoryImpl
import com.yral.shared.features.chat.data.ChatStreamingDataSource
import com.yral.shared.features.chat.data.ConversationContentCache
import com.yral.shared.features.chat.domain.ChatErrorMapper
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.usecases.CheckChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.CreateConversationUseCase
import com.yral.shared.features.chat.domain.usecases.CreateHumanConversationUseCase
import com.yral.shared.features.chat.domain.usecases.DeleteConversationUseCase
import com.yral.shared.features.chat.domain.usecases.GetHumanCreatorTakeoverStatusUseCase
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.features.chat.domain.usecases.GetSoulFileUseCase
import com.yral.shared.features.chat.domain.usecases.GrantChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.UpdateSoulFileUseCase
import com.yral.shared.features.chat.domain.usecases.MarkConversationAsReadUseCase
import com.yral.shared.features.chat.domain.usecases.ReleaseHumanCreatorTakeoverUseCase
import com.yral.shared.features.chat.domain.usecases.SendHumanCreatorMessageUseCase
import com.yral.shared.features.chat.domain.usecases.SendHumanMessageUseCase
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import com.yral.shared.features.chat.domain.usecases.StartHumanCreatorTakeoverUseCase
import com.yral.shared.features.chat.viewmodel.ChatUnreadRefreshSignal
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import com.yral.shared.features.chat.viewmodel.InboxViewModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val chatModule =
    module {
        factoryOf(::ChatRepositoryImpl) bind ChatRepository::class
        factory<ChatDataSource> {
            ChatRemoteDataSource(
                httpClient = get(),
                json = get(),
                preferences = get(),
                chatBaseUrl = get(CHAT_SERVER_BASE_URL),
                influencerFeedBaseUrl = get(INFLUENCER_FEED_SERVER_BASE_URL),
            )
        }
        factory {
            ChatStreamingDataSource(
                httpClient = get(),
                json = get(),
                preferences = get(),
                chatBaseUrl = get(CHAT_SERVER_BASE_URL),
            )
        }
        factory<ChatAccessBillingDataSource> {
            ChatAccessBillingRemoteDataSource(
                httpClient = get(),
                json = get<Json>(),
                packageName = getAppPackageName(),
            )
        }
        factoryOf(::CheckChatAccessUseCase)
        factoryOf(::GrantChatAccessUseCase)
        factoryOf(::CreateConversationUseCase)
        factoryOf(::CreateHumanConversationUseCase)
        factoryOf(::DeleteConversationUseCase)
        factoryOf(::GetInfluencerUseCase)
        factoryOf(::GetSoulFileUseCase)
        factoryOf(::UpdateSoulFileUseCase)
        factoryOf(::MarkConversationAsReadUseCase)
        factoryOf(::SendMessageUseCase)
        factoryOf(::SendHumanMessageUseCase)
        factoryOf(::StartHumanCreatorTakeoverUseCase)
        factoryOf(::ReleaseHumanCreatorTakeoverUseCase)
        factoryOf(::SendHumanCreatorMessageUseCase)
        factoryOf(::GetHumanCreatorTakeoverStatusUseCase)
        factoryOf(::ChatTelemetry)
        singleOf(::ChatErrorMapper)
        singleOf(::ChatUnreadRefreshSignal)
        // Explicit `single { }` (not `singleOf(::…)`) because the constructor has
        // primitive defaults (Int). `singleOf` would try to resolve Int from the
        // Koin graph and crash with NoDefinitionFoundException.
        single { ConversationContentCache() }
        viewModelOf(::ChatWallViewModel)
        viewModel {
            ConversationViewModel(
                flagManager = get(),
                chatRepository = get(),
                useCaseFailureListener = get(),
                sendMessageUseCase = get(),
                sendHumanMessageUseCase = get(),
                createConversationUseCase = get(),
                deleteConversationUseCase = get(),
                markConversationAsReadUseCase = get(),
                shareService = get(),
                urlBuilder = get(),
                linkGenerator = get(),
                crashlyticsManager = get(),
                sessionManager = get(),
                chatTelemetry = get(),
                chatErrorMapper = get(),
                iapManager = get(),
                fetchProductsUseCase = get(),
                checkChatAccessUseCase = get(),
                grantChatAccessUseCase = get(),
                chatUnreadRefreshSignal = get(),
                startHumanCreatorTakeoverUseCase = get(),
                releaseHumanCreatorTakeoverUseCase = get(),
                sendHumanCreatorMessageUseCase = get(),
                getHumanCreatorTakeoverStatusUseCase = get(),
                conversationContentCache = get(),
            )
        }
        viewModelOf(::InboxViewModel)
    }
