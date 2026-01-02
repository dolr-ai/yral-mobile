package com.yral.shared.features.chat.di

import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.data.ChatDataSource
import com.yral.shared.features.chat.data.ChatRemoteDataSource
import com.yral.shared.features.chat.data.ChatRepositoryImpl
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.usecases.CreateConversationUseCase
import com.yral.shared.features.chat.domain.usecases.DeleteConversationUseCase
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import org.koin.core.module.dsl.factoryOf
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
                environmentPrefix =
                    if (get(IS_DEBUG)) {
                        "staging"
                    } else {
                        ""
                    },
            )
        }
        factoryOf(::CreateConversationUseCase)
        factoryOf(::DeleteConversationUseCase)
        factoryOf(::GetInfluencerUseCase)
        factoryOf(::SendMessageUseCase)
        factoryOf(::ChatTelemetry)
        viewModelOf(::ChatWallViewModel)
        viewModelOf(::ConversationViewModel)
    }
