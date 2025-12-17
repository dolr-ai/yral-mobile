package com.yral.shared.features.chat.di

import com.yral.shared.features.chat.data.ChatDataSource
import com.yral.shared.features.chat.data.ChatRemoteDataSource
import com.yral.shared.features.chat.data.ChatRepositoryImpl
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.usecases.CreateConversationUseCase
import com.yral.shared.features.chat.domain.usecases.DeleteConversationUseCase
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.features.chat.viewmodel.ChatConversationsViewModel
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val chatModule =
    module {
        factoryOf(::ChatRepositoryImpl) bind ChatRepository::class
        factoryOf(::ChatRemoteDataSource) bind ChatDataSource::class
        factoryOf(::CreateConversationUseCase)
        factoryOf(::DeleteConversationUseCase)
        factoryOf(::GetInfluencerUseCase)
        viewModelOf(::ChatWallViewModel)
        viewModelOf(::ChatConversationsViewModel)
    }
