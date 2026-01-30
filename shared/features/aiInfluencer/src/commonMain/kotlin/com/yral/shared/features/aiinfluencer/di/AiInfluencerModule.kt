package com.yral.shared.features.aiinfluencer.di

import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.features.aiinfluencer.data.AiInfluencerDataSource
import com.yral.shared.features.aiinfluencer.data.AiInfluencerRemoteDataSource
import com.yral.shared.features.aiinfluencer.data.AiInfluencerRepositoryImpl
import com.yral.shared.features.aiinfluencer.domain.AiInfluencerRepository
import com.yral.shared.features.aiinfluencer.domain.usecases.GeneratePromptUseCase
import com.yral.shared.features.aiinfluencer.domain.usecases.ValidateAndGenerateMetadataUseCase
import com.yral.shared.features.aiinfluencer.viewmodel.AiInfluencerViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val aiInfluencerModule =
    module {
        factoryOf(::AiInfluencerRepositoryImpl) bind AiInfluencerRepository::class
        factory<AiInfluencerDataSource> {
            AiInfluencerRemoteDataSource(
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
        factoryOf(::GeneratePromptUseCase)
        factoryOf(::ValidateAndGenerateMetadataUseCase)
        viewModelOf(::AiInfluencerViewModel)
    }
