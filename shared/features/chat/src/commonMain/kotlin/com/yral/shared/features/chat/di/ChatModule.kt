package com.yral.shared.features.chat.di

import com.yral.shared.core.di.CHAT_SERVER_BASE_URL
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.data.ChatAccessBillingDataSource
import com.yral.shared.features.chat.data.ChatAccessBillingRemoteDataSource
import com.yral.shared.features.chat.data.ChatDataSource
import com.yral.shared.features.chat.data.ChatRemoteDataSource
import com.yral.shared.features.chat.data.ChatRepositoryImpl
import com.yral.shared.features.chat.domain.ChatErrorMapper
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.usecases.CheckChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.CreateConversationUseCase
import com.yral.shared.features.chat.domain.usecases.DeleteConversationUseCase
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.features.chat.domain.usecases.GrantChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.MarkConversationAsReadUseCase
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import com.yral.shared.features.chat.viewmodel.InboxViewModel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
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
        factoryOf(::DeleteConversationUseCase)
        factoryOf(::GetInfluencerUseCase)
        factoryOf(::MarkConversationAsReadUseCase)
        factoryOf(::SendMessageUseCase)
        factoryOf(::ChatTelemetry)
        singleOf(::ChatErrorMapper)
        viewModelOf(::ChatWallViewModel)
        viewModelOf(::ConversationViewModel)
        viewModelOf(::InboxViewModel)
    }
